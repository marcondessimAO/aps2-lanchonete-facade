package br.edu.aps2.lanchonete;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class Reais {
    private Reais() {
    }

    static String formatar(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                .format(valor).replace('\u00a0', ' ');
    }
}
