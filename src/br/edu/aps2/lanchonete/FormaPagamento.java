package br.edu.aps2.lanchonete;

public enum FormaPagamento {
    PIX("Pix"),
    CARTAO("Cartão");

    private final String nome;

    FormaPagamento(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }
}
