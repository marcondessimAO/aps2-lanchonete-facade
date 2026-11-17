package br.edu.aps2.lanchonete;

import java.math.BigDecimal;

public enum Produto {
    HAMBURGUER("Hambúrguer", "15.00"),
    BATATA("Batata frita", "8.50"),
    REFRIGERANTE("Refrigerante", "6.00"),
    SUCO("Suco", "5.50");

    private final String nome;
    private final BigDecimal preco;

    Produto(String nome, String preco) {
        this.nome = nome;
        this.preco = new BigDecimal(preco);
    }

    public String nome() {
        return nome;
    }

    public BigDecimal preco() {
        return preco;
    }
}
