package br.edu.aps2.lanchonete;

import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        try (Scanner entrada = new Scanner(System.in)) {
            new MenuTerminal(entrada, Configuracao.criarFachada()).executar();
        }
    }
}
