package com.achadosedevolvidos.auth.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera o token bruto de redefinição de senha: uma string aleatória opaca, não um
 * JWT — um JWT vaza o e-mail do usuário em texto legível no payload, ruim para algo
 * que trafega por e-mail/logs de servidor de e-mail.
 */
public final class SecureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private SecureTokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
