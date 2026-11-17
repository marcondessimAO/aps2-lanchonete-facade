package br.edu.aps2.lanchonete;

import java.math.BigDecimal;
import java.util.Objects;

public final class PedidoFacade {
    private final Estoque estoque;
    private final ProcessadorPagamento pagamento;
    private final EmissorComprovante comprovante;

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

        var indisponivel = estoque.primeiroItemIndisponivel(pedido);
        if (indisponivel.isPresent()) {
            Pedido.Item item = indisponivel.get();
            return new ResultadoPedido(false, "Estoque insuficiente para %s. Pedido: %d. Disponível: %d."
                    .formatted(item.produto().nome(), item.quantidade(), quantidadeDisponivel(item.produto())));
        }

        BigDecimal total = pedido.total();
        if (!pagamento.processar(forma, total)) {
            return new ResultadoPedido(false, "Pagamento recusado. O pedido permanece no carrinho.");
        }

        estoque.baixar(pedido);
        return new ResultadoPedido(true, comprovante.emitir(pedido, forma, total));
    }
}
