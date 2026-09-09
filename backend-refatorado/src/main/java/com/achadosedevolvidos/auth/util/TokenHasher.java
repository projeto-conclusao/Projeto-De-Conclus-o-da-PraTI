package com.achadosedevolvidos.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 do token guardado no banco (refresh token JWT ou token de reset de
 * senha) — nunca o valor bruto. Reaproveitado pelos dois porque nenhum dos dois é
 * um segredo de baixa entropia (ao contrário de senha), então um hash rápido e sem
 * salt já é suficiente: o objetivo é só impedir que um dump do banco entregue
 * credenciais utilizáveis, não resistir a força bruta sobre o próprio token.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
