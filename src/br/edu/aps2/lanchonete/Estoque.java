package br.edu.aps2.lanchonete;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class Estoque {
    private final Map<Produto, Integer> quantidades = new EnumMap<>(Produto.class);

    public Estoque(Map<Produto, Integer> quantidadesIniciais) {
        quantidadesIniciais.forEach((produto, quantidade) -> {
            if (quantidade < 0) {
                throw new IllegalArgumentException("O estoque inicial precisa ser não negativo.");
            }
            quantidades.put(produto, quantidade);
        });
    }

    public int quantidadeDisponivel(Produto produto) {
        return quantidades.getOrDefault(produto, 0);
    }

    public Optional<Pedido.Item> primeiroItemIndisponivel(Pedido pedido) {
        return pedido.itens().stream()
                .filter(item -> item.quantidade() > quantidadeDisponivel(item.produto()))
                .findFirst();
    }

    void baixar(Pedido pedido) {
        for (Pedido.Item item : pedido.itens()) {
            Produto produto = item.produto();
            quantidades.put(produto, quantidadeDisponivel(produto) - item.quantidade());
        }
    }
}
