package br.edu.aps2.lanchonete;

import java.util.EnumMap;
import java.util.Map;
import java.util.Scanner;

public final class MenuTerminal {
    private final Map<Produto, Integer> carrinho = new EnumMap<>(Produto.class);
    private final Scanner entrada;
    private final PedidoFacade fachada;

    public MenuTerminal(Scanner entrada, PedidoFacade fachada) {
        this.entrada = entrada;
        this.fachada = fachada;
    }

    public void executar() {
        System.out.println("Bem-vindo à Lanchonete APS 2!");
        while (true) {
            System.out.println("""

                    1. Ver cardápio e estoque
                    2. Adicionar ao carrinho
                    3. Ver carrinho
                    4. Remover produto do carrinho
                    5. Finalizar pedido
                    0. Sair""");
            switch (lerNumero("Opção: ", 0, 5)) {
                case 1 -> exibirCardapio();
                case 2 -> adicionarProduto();
                case 3 -> exibirCarrinho();
                case 4 -> removerProduto();
                case 5 -> finalizarPedido();
                case 0 -> {
                    System.out.println("Até logo!");
                    return;
                }
                default -> throw new IllegalStateException("Opção fora do menu.");
            }
        }
    }

    private void exibirCardapio() {
        System.out.println("\n--- Cardápio ---");
        for (Produto produto : Produto.values()) {
            System.out.printf("%d. %s | %s | estoque: %d%n",
                    produto.ordinal() + 1, produto.nome(), Reais.formatar(produto.preco()),
                    fachada.quantidadeDisponivel(produto));
        }
    }

    private void adicionarProduto() {
        Produto[] produtos = Produto.values();
        exibirCardapio();
        int codigo = lerNumero("Produto (0 para voltar): ", 0, produtos.length);
        if (codigo == 0) {
            return;
        }

        Produto produto = produtos[codigo - 1];
        int noCarrinho = carrinho.getOrDefault(produto, 0);
        int quantidade = lerNumero("Quantidade (0 para voltar): ", 0, Integer.MAX_VALUE - noCarrinho);
        if (quantidade > 0) {
            carrinho.put(produto, noCarrinho + quantidade);
            System.out.println("Produto adicionado ao carrinho.");
        }
    }

    private void removerProduto() {
        exibirCarrinho();
        if (carrinho.isEmpty()) {
            return;
        }

        Produto[] produtos = Produto.values();
        int codigo = lerNumero("Código do produto a remover (0 para voltar): ", 0, produtos.length);
        if (codigo == 0) {
            return;
        }

        if (carrinho.remove(produtos[codigo - 1]) == null) {
            System.out.println("Esse produto não está no carrinho.");
            return;
        }
        System.out.println("Produto removido do carrinho.");
    }

    private void exibirCarrinho() {
        if (carrinho.isEmpty()) {
            System.out.println("O carrinho está vazio.");
            return;
        }

        Pedido pedido = new Pedido(carrinho);
        System.out.println("\n--- Carrinho ---");
        for (Pedido.Item item : pedido.itens()) {
            System.out.printf("%d. %s | %d un. | %s%n", item.produto().ordinal() + 1,
                    item.produto().nome(), item.quantidade(), Reais.formatar(item.subtotal()));
        }
        System.out.println("Total: " + Reais.formatar(pedido.total()));
    }

    private void finalizarPedido() {
        exibirCarrinho();
        if (carrinho.isEmpty()) {
            return;
        }

        System.out.println("\nPagamento simulado");
        System.out.println("1. Pix");
        System.out.println("2. Cartão");
        int opcao = lerNumero("Forma de pagamento (0 para voltar): ", 0, 2);
        if (opcao == 0) {
            return;
        }

        FormaPagamento forma = opcao == 1 ? FormaPagamento.PIX : FormaPagamento.CARTAO;
        ResultadoPedido resultado = fachada.finalizarPedido(new Pedido(carrinho), forma);
        System.out.println(resultado.mensagem());
        if (resultado.aprovado()) {
            carrinho.clear();
        }
    }

    private int lerNumero(String mensagem, int minimo, int maximo) {
        while (true) {
            System.out.print(mensagem);
            if (!entrada.hasNextLine()) {
                return 0;
            }

            try {
                int numero = Integer.parseInt(entrada.nextLine().strip());
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
            } catch (NumberFormatException ignorada) {
                // O mesmo aviso vale para texto e números fora do intervalo.
            }
            System.out.printf("Digite um número inteiro entre %d e %d.%n", minimo, maximo);
        }
    }
}
