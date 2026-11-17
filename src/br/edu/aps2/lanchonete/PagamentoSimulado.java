package br.edu.aps2.lanchonete;

import java.math.BigDecimal;

public final class PagamentoSimulado implements ProcessadorPagamento {
    private static final BigDecimal LIMITE_CARTAO = new BigDecimal("30.00");

    @Override
    public boolean processar(FormaPagamento forma, BigDecimal valor) {
        return switch (forma) {
            case PIX -> true;
            case CARTAO -> valor.compareTo(LIMITE_CARTAO) <= 0;
        };
    }
}
