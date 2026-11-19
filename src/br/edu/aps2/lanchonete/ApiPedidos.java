package br.edu.aps2.lanchonete;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Recebe as requisições HTTP da interface web e delega o fechamento à fachada.
 * A resposta JSON leva o resultado da venda e o cardápio atualizado.
 */
public final class ApiPedidos {
    private static final int LIMITE_CORPO = 1024;
    private static final String FORMULARIO = "application/x-www-form-urlencoded";

    private final PedidoFacade fachada;
    // Guarda vendas aprovadas por identificador de tentativa enquanto a API estiver rodando.
    private final Map<UUID, Venda> vendas = new HashMap<>();

    private ApiPedidos(PedidoFacade fachada) {
        this.fachada = fachada;
    }

    public static void main(String[] args) throws IOException {
        int porta = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        ApiPedidos api = new ApiPedidos(Configuracao.criarFachada());
        // api::atender indica o método chamado pelo servidor para os caminhos sob /api/.
        servidor.createContext("/api/", api::atender);
        // Uma requisição por vez impede que outra venda intervenha entre a conferência e a baixa.
        servidor.setExecutor(Executors.newSingleThreadExecutor());
        servidor.start();
        System.out.printf("API da lanchonete em http://127.0.0.1:%d/api/cardapio%n", servidor.getAddress().getPort());
    }

    private void atender(HttpExchange troca) throws IOException {
        // Fecha a troca HTTP ao final, inclusive se ocorrer uma exceção.
        try (troca) {
            switch (troca.getRequestURI().getPath()) {
                case "/api/cardapio" -> {
                    if (aceitarMetodo(troca, "GET")) {
                        enviar(troca, 200, cardapio());
                    }
                }
                case "/api/pedidos" -> {
                    if (aceitarMetodo(troca, "POST")) {
                        finalizar(troca);
                    }
                }
                default -> erro(troca, 404, "Rota não encontrada.");
            }
        }
    }

    private void finalizar(HttpExchange troca) throws IOException {
        String tipo = troca.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !FORMULARIO.equalsIgnoreCase(tipo.split(";", 2)[0].strip())) {
            erro(troca, 415, "Envie os dados como formulário.");
            return;
        }

        // O byte extra permite detectar um corpo que ultrapassa o limite.
        byte[] corpo = troca.getRequestBody().readNBytes(LIMITE_CORPO + 1);
        if (corpo.length > LIMITE_CORPO) {
            erro(troca, 413, "Pedido maior que o limite permitido.");
            return;
        }

        Solicitacao solicitacao;
        try {
            solicitacao = lerFormulario(new String(corpo, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException invalido) {
            erro(troca, 400, "Confira o identificador, a forma de pagamento e os itens do pedido.");
            return;
        }

        // Ao repetir uma venda aprovada com os mesmos dados, reutiliza o resultado sem dar outra baixa.
        Venda venda = vendas.get(solicitacao.tentativa());
        if (venda != null && !venda.solicitacao().equals(solicitacao)) {
            erro(troca, 409, "Este identificador já foi usado em outro pedido.");
            return;
        }

        ResultadoPedido resultado;
        if (venda == null) {
            // Converte as quantidades recebidas em um Pedido e entrega a venda à fachada.
            Pedido pedido = new Pedido(solicitacao.quantidades());
            resultado = fachada.finalizarPedido(pedido, solicitacao.pagamento());
            if (resultado.aprovado()) {
                vendas.put(solicitacao.tentativa(), new Venda(solicitacao, resultado));
            }
        } else {
            resultado = venda.resultado();
        }

        // O status 200 indica que a API processou a solicitação. O campo aprovado informa se houve venda.
        enviar(troca, 200, """
                {"aprovado":%s,"mensagem":%s,"produtos":%s}"""
                .formatted(resultado.aprovado(), textoJson(resultado.mensagem()), cardapio()));
    }

    // Separa o identificador e o pagamento. Os campos restantes são códigos de produto e quantidades.
    private static Solicitacao lerFormulario(String corpo) {
        Map<String, String> campos = new HashMap<>();
        for (String par : corpo.split("&", -1)) {
            String[] campo = par.split("=", 2);
            if (campo.length != 2 || campos.putIfAbsent(
                    URLDecoder.decode(campo[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(campo[1], StandardCharsets.UTF_8)) != null) {
                throw new IllegalArgumentException("Campo inválido ou duplicado.");
            }
        }

        String tentativa = campos.remove("tentativa");
        String pagamento = campos.remove("pagamento");
        if (tentativa == null || pagamento == null) {
            throw new IllegalArgumentException("Identificador e pagamento são obrigatórios.");
        }
        Map<Produto, Integer> quantidades = new EnumMap<>(Produto.class);
        campos.forEach((codigo, valor) -> {
            int quantidade = Integer.parseInt(valor);
            if (quantidade <= 0) {
                throw new IllegalArgumentException("Quantidade inválida.");
            }
            quantidades.put(Produto.valueOf(codigo), quantidade);
        });
        return new Solicitacao(UUID.fromString(tentativa), FormaPagamento.valueOf(pagamento), quantidades);
    }

    private String cardapio() {
        return Arrays.stream(Produto.values())
                .map(produto -> """
                        {"id":%s,"nome":%s,"precoCentavos":%d,"estoque":%d}"""
                        .formatted(textoJson(produto.name()), textoJson(produto.nome()),
                                produto.preco().movePointRight(2).longValueExact(),
                                fachada.quantidadeDisponivel(produto)))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static boolean aceitarMetodo(HttpExchange troca, String metodo) throws IOException {
        if (metodo.equals(troca.getRequestMethod())) {
            return true;
        }
        troca.getResponseHeaders().set("Allow", metodo);
        erro(troca, 405, "Método não permitido.");
        return false;
    }

    private static void erro(HttpExchange troca, int status, String mensagem) throws IOException {
        enviar(troca, status, "{\"mensagem\":" + textoJson(mensagem) + "}");
    }

    private static void enviar(HttpExchange troca, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.sendResponseHeaders(status, bytes.length);
        troca.getResponseBody().write(bytes);
    }

    private static String textoJson(String texto) {
        StringBuilder json = new StringBuilder("\"");
        for (char caractere : texto.toCharArray()) {
            switch (caractere) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                default -> {
                    if (caractere < 32) {
                        json.append("\\u%04x".formatted((int) caractere));
                    } else {
                        json.append(caractere);
                    }
                }
            }
        }
        return json.append('"').toString();
    }

    private record Solicitacao(UUID tentativa, FormaPagamento pagamento, Map<Produto, Integer> quantidades) {
    }

    private record Venda(Solicitacao solicitacao, ResultadoPedido resultado) {
    }
}
