package br.edu.aps2.lanchonete;

import java.util.Map;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        Estoque estoque = new Estoque(Map.of(
                Produto.HAMBURGUER, 8,
                Produto.BATATA, 6,
                Produto.REFRIGERANTE, 10,
                Produto.SUCO, 5));
        PedidoFacade fachada = new PedidoFacade(
                estoque, new PagamentoSimulado(), new EmissorComprovante());

        try (Scanner entrada = new Scanner(System.in)) {
            new MenuTerminal(entrada, fachada).executar();
        }
    }
}
