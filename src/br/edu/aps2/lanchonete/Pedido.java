package br.edu.aps2.lanchonete;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Pedido {
    private final List<Item> itens;

    public Pedido(Map<Produto, Integer> quantidades) {
        itens = quantidades.entrySet().stream()
                .map(entrada -> new Item(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    public List<Item> itens() {
        return itens;
    }

    public BigDecimal total() {
        return itens.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record Item(Produto produto, int quantidade) {
        public Item {
            Objects.requireNonNull(produto);
            if (quantidade <= 0) {
                throw new IllegalArgumentException("A quantidade precisa ser positiva.");
            }
        }

        public BigDecimal subtotal() {
            return produto.preco().multiply(BigDecimal.valueOf(quantidade));
        }
    }
}
