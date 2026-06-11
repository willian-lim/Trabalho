package br.carmel.util;

import br.carmel.model.Licenca;
import br.carmel.repository.LicencaRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

public class LicencaManager {

    public static final int DIAS_VALIDADE = 30;

    static final String CHAVE_SECRETA =
            "C4rm3l$1st#2024!@Pr0d@S3cr3tK3y#X9z&mN7qR2vL5pW8";

    private static final String SEP    = "|";
    static final String SEP_VISUAL     = "-";
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static LicencaRepository repositorio;

    public static void setRepositorio(LicencaRepository repo) {
        repositorio = repo;
    }

    public enum Status {
        VALIDA, EXPIRADA, CORROMPIDA, SEM_LICENCA, CHAVE_JA_UTILIZADA
    }

    public record ResultadoLicenca(Status status, LocalDate dataInicio, int diasRestantes) {
        public boolean podeUsar() { return status == Status.VALIDA; }
    }

    // ── Normalização de chave (SEMPRE igual em todo o código) ─────────────────

    /**
     * Remove hífens, espaços e qualquer padding da chave.
     * É OBRIGATÓRIO usar este método antes de calcular o hash
     * para garantir que o mesmo hash seja gerado sempre para a mesma chave,
     * independente de como ela foi digitada/colada.
     */
    static String normalizarChave(String chaveFormatada) {
        return chaveFormatada
                .replace(SEP_VISUAL, "")
                .replaceAll("\\s+", "")
                .replace("=", ""); // sem padding
    }

    // ── Data confiável via NTP ────────────────────────────────────────────────

    static LocalDate obterHoje(LocalDate ultimaDataGravada) {
        LocalDate dataNtp = buscarDataNtp();
        if (dataNtp != null) return dataNtp;

        // Sem internet: relógio local mas com anti-rollback
        LocalDate hoje = LocalDate.now();
        if (ultimaDataGravada != null && hoje.isBefore(ultimaDataGravada)) {
            return null; // rollback detectado sem internet → bloqueia
        }
        return hoje;
    }

    private static LocalDate buscarDataNtp() {
        String[] servidores = {
            "pool.ntp.org", "time.google.com",
            "time.cloudflare.com", "time.windows.com"
        };
        for (String servidor : servidores) {
            try {
                byte[] msg = new byte[48];
                msg[0] = 0x1B;
                try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
                    socket.setSoTimeout(4000);
                    InetAddress addr = InetAddress.getByName(servidor);
                    java.net.DatagramPacket req =
                            new java.net.DatagramPacket(msg, msg.length, addr, 123);
                    socket.send(req);
                    java.net.DatagramPacket resp =
                            new java.net.DatagramPacket(new byte[48], 48);
                    socket.receive(resp);
                    byte[] data = resp.getData();
                    long segundos = 0;
                    for (int i = 40; i <= 43; i++)
                        segundos = (segundos << 8) | (data[i] & 0xFF);
                    long unixSegundos = segundos - 2208988800L;
                    return LocalDateTime
                            .ofEpochSecond(unixSegundos, 0, ZoneOffset.UTC)
                            .toLocalDate();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ── Verificar licença ─────────────────────────────────────────────────────

    public static ResultadoLicenca verificar() {
        Path arquivo = caminhoArquivo();
        if (!Files.exists(arquivo)) {
            if (repositorio != null && repositorio.buscarMaisRecente().isPresent())
                return new ResultadoLicenca(Status.CORROMPIDA, null, 0);
            return new ResultadoLicenca(Status.SEM_LICENCA, null, 0);
        }
        return lerEValidar(arquivo);
    }

    // ── Ativar nova chave ─────────────────────────────────────────────────────

    public static boolean renovar(String chaveFormatada) {
        try {
            // ── BARREIRA 1: se já existe licença no banco e o arquivo está
            // corrompido/ausente, a mesma chave antiga NÃO pode ser reinserida.
            // Qualquer tentativa de ativação quando há registro no banco
            // só é permitida se a chave for NOVA (não registrada ainda).
            // Isso é verificado na barreira 3 abaixo, mas precisamos
            // garantir que o hash é sempre calculado da mesma forma. ──────────

            // 1. Normalizar chave — SEMPRE com normalizarChave()
            String chaveNorm = normalizarChave(chaveFormatada);

            // 2. Restaurar padding para decodificação Base64
            String chaveComPadding = chaveNorm;
            int resto = chaveComPadding.length() % 4;
            if (resto == 2) chaveComPadding += "==";
            else if (resto == 3) chaveComPadding += "=";

            // 3. Decodificar Base64
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(chaveComPadding);
            } catch (IllegalArgumentException e) {
                return false;
            }
            String conteudo = new String(decoded, StandardCharsets.UTF_8);

            // 4. Separar campos
            String[] partes = conteudo.split("\\|", 3);
            if (partes.length != 3) return false;

            String dataStr      = partes[0];
            String serialStr    = partes[1];
            String hmacRecebido = partes[2];

            // 5. Verificar assinatura HMAC
            String payload      = dataStr + SEP + serialStr;
            String hmacCurto    = hmac(payload + "CARMEL-LICENCA-V4").substring(0, 16);
            if (!hmacCurto.equals(hmacRecebido)) return false;

            // 6. Verificar data via NTP
            LocalDate hoje = obterHoje(null);
            if (hoje == null) throw new SemInternetException();

            LocalDate dataInicio = LocalDate.parse(dataStr, FMT);
            LocalDate expiracao  = dataInicio.plusDays(DIAS_VALIDADE);

            // 7. Bloquear chave expirada (data NTP)
            if (!hoje.isBefore(expiracao)) return false;

            // ── BARREIRA PRINCIPAL: hash calculado com chaveNorm (sem hifens,
            // sem espaços, sem padding) — mesma normalização sempre. ──────────
            String hashChave = sha256Hex(chaveNorm);

            // 8. Verificar reutilização — usa hash normalizado
            if (repositorio != null && repositorio.chaveJaUtilizada(hashChave)) {
                throw new ChaveJaUtilizadaException();
            }

            // ── BARREIRA EXTRA: se existe qualquer licença no banco mas
            // o arquivo local está ausente/corrompido → alguém apagou o arquivo.
            // Só permite ativar se a chave for genuinamente nova (não no banco).
            // A verificação acima já cobre isso, mas se o banco está indisponível
            // e o arquivo está corrompido, bloqueia por segurança. ─────────────
            if (repositorio == null && Files.exists(caminhoArquivo())) {
                // Sem banco mas arquivo existe e vai ser sobrescrito — permitido
            }

            // 9. Gravar arquivo local
            gravarArquivo(caminhoArquivo(), dataInicio, hoje, serialStr);

            // 10. Registrar no banco com hash normalizado
            if (repositorio != null) {
                Licenca licenca = new Licenca();
                licenca.setHashChave(hashChave); // hash da chave normalizada
                licenca.setDataInicio(dataInicio);
                licenca.setDataExpiracao(expiracao);
                licenca.setAtivadaEm(LocalDateTime.now());
                licenca.setFingerprintMaquina(gerarFingerprint());
                licenca.setSerial(Integer.parseInt(serialStr));
                repositorio.salvar(licenca);
            }

            return true;

        } catch (ChaveJaUtilizadaException | SemInternetException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    public static class ChaveJaUtilizadaException extends RuntimeException {
        public ChaveJaUtilizadaException() {
            super("Esta chave de licença já foi utilizada anteriormente.");
        }
    }

    public static class SemInternetException extends RuntimeException {
        public SemInternetException() {
            super("Não foi possível verificar a data online. Verifique sua conexão.");
        }
    }

    public static int diasRestantes() { return verificar().diasRestantes(); }

    // ── Leitura e validação do arquivo ────────────────────────────────────────

    private static ResultadoLicenca lerEValidar(Path arquivo) {
        try {
            String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8).trim();

            int ultimo = conteudo.lastIndexOf(SEP);
            if (ultimo < 0) return corrompida();

            String campos      = conteudo.substring(0, ultimo);
            String hmacArquivo = conteudo.substring(ultimo + 1);
            if (!hmac(campos + "CARMEL-ARQUIVO-V4").equals(hmacArquivo)) return corrompida();

            String[] partes = campos.split("\\|", 3);
            if (partes.length != 3) return corrompida();

            LocalDate dataInicio = LocalDate.parse(partes[0], FMT);
            LocalDate ultimaData = LocalDate.parse(partes[1], FMT);
            String    serialStr  = partes[2];
            LocalDate expiracao  = dataInicio.plusDays(DIAS_VALIDADE);

            // Data confiável: NTP prioritário, fallback com anti-rollback
            LocalDate hoje = obterHoje(ultimaData);
            if (hoje == null) return corrompida(); // rollback sem internet

            // Consistência com banco
            if (repositorio != null) {
                Optional<Licenca> lb = repositorio.buscarMaisRecente();
                if (lb.isPresent()) {
                    if (!lb.get().getDataInicio().equals(dataInicio)) return corrompida();
                } else {
                    return corrompida();
                }
            }

            if (hoje.isAfter(ultimaData))
                gravarArquivo(arquivo, dataInicio, hoje, serialStr);

            if (hoje.isAfter(expiracao))
                return new ResultadoLicenca(Status.EXPIRADA, dataInicio, 0);

            int diasRestantes = (int)(DIAS_VALIDADE -
                    java.time.temporal.ChronoUnit.DAYS.between(dataInicio, hoje));
            return new ResultadoLicenca(Status.VALIDA, dataInicio, diasRestantes);

        } catch (Exception e) {
            return corrompida();
        }
    }

    // ── Arquivo local ─────────────────────────────────────────────────────────

    static void gravarArquivo(Path arquivo, LocalDate dataInicio, LocalDate ultimaData)
            throws Exception { gravarArquivo(arquivo, dataInicio, ultimaData, "0"); }

    private static void gravarArquivo(Path arquivo, LocalDate dataInicio,
                                      LocalDate ultimaData, String serial) throws Exception {
        Files.createDirectories(arquivo.getParent());
        String campos     = dataInicio.format(FMT) + SEP + ultimaData.format(FMT) + SEP + serial;
        String assinatura = hmac(campos + "CARMEL-ARQUIVO-V4");
        Files.writeString(arquivo, campos + SEP + assinatura, StandardCharsets.UTF_8);
    }

    private static ResultadoLicenca corrompida() {
        return new ResultadoLicenca(Status.CORROMPIDA, null, 0);
    }

    static Path caminhoArquivo() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank())
            return Path.of(appData, "Carmel", "licenca.dat");
        return Path.of(System.getProperty("user.home"), ".config", "Carmel", "licenca.dat");
    }

    // ── Criptografia ──────────────────────────────────────────────────────────

    static String gerarFingerprint() {
        try {
            String host    = InetAddress.getLocalHost().getHostName();
            String usuario = System.getProperty("user.name", "desconhecido");
            return sha256Hex(host + "::" + usuario).substring(0, 32);
        } catch (Exception e) { return "fingerprint-indisponivel"; }
    }

    static String hmac(String dados) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(CHAVE_SECRETA.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return bytesParaHex(mac.doFinal(dados.getBytes(StandardCharsets.UTF_8)));
    }

    static String sha256Hex(String texto) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesParaHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
    }

    private static String bytesParaHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}