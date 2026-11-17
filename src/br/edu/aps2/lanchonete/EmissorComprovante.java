package br.edu.aps2.lanchonete;

import java.math.BigDecimal;

public final class EmissorComprovante {
    public String emitir(Pedido pedido, FormaPagamento forma, BigDecimal total) {
        StringBuilder comprovante = new StringBuilder("Pedido aprovado!\n--- Comprovante ---\n");
        for (Pedido.Item item : pedido.itens()) {
            comprovante.append("%d x %s = %s%n".formatted(
                    item.quantidade(), item.produto().nome(), Reais.formatar(item.subtotal())));
        }
        comprovante.append("Total: %s%nPagamento: %s".formatted(
                Reais.formatar(total), forma.nome()));
        return comprovante.toString();
    }
}
