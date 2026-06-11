package br.carmel.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class LicencaGerador {

    private static final DateTimeFormatter FMT = LicencaManager.FMT;
    private static final String SEP            = "|";

    public static void main(String[] args) throws Exception {
        exibirBanner();
        Scanner sc = new Scanner(System.in);

        System.out.println("  Data de início (YYYY-MM-DD)");
        System.out.print("  [Enter = hoje]: ");
        String entradaData = sc.nextLine().trim();

        System.out.print("\n  Quantidade de chaves [1]: ");
        String entradaQtd = sc.nextLine().trim();

        LocalDate dataInicio = entradaData.isEmpty()
                ? LocalDate.now() : LocalDate.parse(entradaData, FMT);
        LocalDate dataFim    = dataInicio.plusDays(LicencaManager.DIAS_VALIDADE);

        int quantidade = entradaQtd.isEmpty() ? 1 : Integer.parseInt(entradaQtd);
        quantidade = Math.max(1, Math.min(quantidade, 50));

        System.out.println();
        System.out.println("  ┌──────────────────────────────────────────────────┐");
        System.out.printf ("  │  Validade: %s → %s (%d dias)%n",
                dataInicio.format(FMT), dataFim.format(FMT), LicencaManager.DIAS_VALIDADE);
        System.out.println("  └──────────────────────────────────────────────────┘");
        System.out.println();

        for (int i = 0; i < quantidade; i++) {
            int    serial = gerarSerial();
            String chave  = gerarChave(dataInicio, serial);
            System.out.printf("  [%02d]  Serial #%07d%n", i + 1, serial);
            System.out.println("        " + chave);
            System.out.println();
        }

        System.out.println("  Cada chave é válida por " + LicencaManager.DIAS_VALIDADE
                + " dias e pode ser usada UMA única vez.");
        System.out.println();
    }

    
    public static String gerarChave(LocalDate dataInicio, int serial) throws Exception {
        String dataStr  = dataInicio.format(FMT);
        String payload  = dataStr + SEP + serial;

        
        String hmacCurto = LicencaManager.hmac(payload + "CARMEL-LICENCA-V4").substring(0, 16);
        String conteudo  = payload + SEP + hmacCurto;

        
        String base64 = Base64.getEncoder()
                .encodeToString(conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .replace("=", "");

        
        return formatarEmGrupos(base64, 5, LicencaManager.SEP_VISUAL);
    }

    public static int gerarSerial() {
        return ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
    }

    private static String formatarEmGrupos(String texto, int tam, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            if (i > 0 && i % tam == 0) sb.append(sep);
            sb.append(texto.charAt(i));
        }
        return sb.toString();
    }

    private static void exibirBanner() {
        System.out.println();
        System.out.println("  ╔════════════════════════════════════════════╗");
        System.out.println("  ║   CARMEL SISTEMA — Gerador de Licença     ║");
        System.out.println("  ║              Uso Interno                  ║");
        System.out.println("  ╚════════════════════════════════════════════╝");
        System.out.println();
    }
}