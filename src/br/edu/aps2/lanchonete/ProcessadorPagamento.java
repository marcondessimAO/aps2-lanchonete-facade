package br.edu.aps2.lanchonete;

import java.math.BigDecimal;

@FunctionalInterface
public interface ProcessadorPagamento {
    /**
     * Processa o valor positivo na forma selecionada.
     * Retorna true quando o pagamento foi aprovado e false quando foi recusado.
     */
    boolean processar(FormaPagamento forma, BigDecimal valor);
}
