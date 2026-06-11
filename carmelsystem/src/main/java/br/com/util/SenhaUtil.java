package br.carmel.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class SenhaUtil {

    
    public static final String SENHA_ADMIN = "matheus";

    private SenhaUtil() {}

    
    public static String hash(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash da senha", e);
        }
    }

    
    public static boolean verificar(String senhaTexto, String hashArmazenado) {
        return hash(senhaTexto).equals(hashArmazenado);
    }

    
    public static boolean isAdmin(String senhaTexto) {
        return SENHA_ADMIN.equals(senhaTexto);
    }
}