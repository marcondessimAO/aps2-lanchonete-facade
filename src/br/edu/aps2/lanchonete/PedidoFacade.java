package br.edu.aps2.lanchonete;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Oferece à API e ao menu uma operação de fechamento. Cada componente executa
 * sua parte e a fachada define a ordem das chamadas.
 */
public final class PedidoFacade {
    private final Estoque estoque;
    private final ProcessadorPagamento pagamento;
    private final EmissorComprovante comprovante;

    /**
     * Recebe as dependências já montadas em Configuracao. Para o pagamento,
     * aceita implementações de ProcessadorPagamento. Estoque e
     * EmissorComprovante são os tipos concretos exigidos pelo construtor.
     */
    public PedidoFacade(Estoque estoque, ProcessadorPagamento pagamento, EmissorComprovante comprovante) {
        this.estoque = Objects.requireNonNull(estoque);
        this.pagamento = Objects.requireNonNull(pagamento);
        this.comprovante = Objects.requireNonNull(comprovante);
    }

    public int quantidadeDisponivel(Produto produto) {
        return estoque.quantidadeDisponivel(produto);
    }

    public ResultadoPedido finalizarPedido(Pedido pedido, FormaPagamento forma) {
        Objects.requireNonNull(forma);
        if (pedido.itens().isEmpty()) {
            return new ResultadoPedido(false, "O carrinho está vazio.");
        }

        // O Optional contém o primeiro item sem quantidade suficiente, se houver.
        // Retornar aqui evita solicitar pagamento por um pedido que o estoque não atende.
        var indisponivel = estoque.primeiroItemIndisponivel(pedido);
        if (indisponivel.isPresent()) {
            Pedido.Item item = indisponivel.get();
            return new ResultadoPedido(false, "Estoque insuficiente para %s. Pedido: %d. Disponível: %d."
                    .formatted(item.produto().nome(), item.quantidade(), quantidadeDisponivel(item.produto())));
        }

        BigDecimal total = pedido.total();
        // O processador devolve false na recusa. O retorno antecipado evita a baixa.
        if (!pagamento.processar(forma, total)) {
            return new ResultadoPedido(false, "Pagamento recusado. O pedido permanece no carrinho.");
        }

        // Só uma venda aprovada chega à baixa e à emissão do comprovante.
        estoque.baixar(pedido);
        return new ResultadoPedido(true, comprovante.emitir(pedido, forma, total));
    }
}
