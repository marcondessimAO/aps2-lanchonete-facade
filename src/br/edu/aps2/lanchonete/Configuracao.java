package br.edu.aps2.lanchonete;

import java.util.Map;

final class Configuracao {
    private Configuracao() {
    }

    static PedidoFacade criarFachada() {
        Estoque estoque = new Estoque(Map.of(
                Produto.HAMBURGUER, 8,
                Produto.BATATA, 6,
                Produto.REFRIGERANTE, 10,
                Produto.SUCO, 5));
        return new PedidoFacade(estoque, new PagamentoSimulado(), new EmissorComprovante());
    }
}
