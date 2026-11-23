package com.hanttracker.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Random passwords, codes and tokens, and the hashes they are stored as. */
public final class Secrets {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Letters and digits that can't be mistaken for each other when read aloud or copied. */
    private static final String PASSWORD_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private Secrets() {}

    /** A one-time password for an admin to pass on: 12 characters, about 70 bits. */
    public static String temporaryPassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    /** A six-digit code to type in from an email. */
    public static String code() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    /** A token for a link: 32 random bytes, URL-safe. */
    public static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256, hex: enough for random codes and tokens, which aren't guessable like passwords. */
    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Compares two hashes in constant time. */
    public static boolean sameHash(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
