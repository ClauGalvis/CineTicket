package com.cineticket.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {
    private static final int ROUNDS = 12;

    private PasswordUtil() { }

    public static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("password null");
        String salt = BCrypt.gensalt(ROUNDS);
        return BCrypt.hashpw(password, salt);
    }

    public static boolean verificarPassword(String password, String hash) {
        if (password == null || hash == null) return false;
        return BCrypt.checkpw(password, hash);
    }

    public static boolean validarFortaleza(String password) {
        if (password == null) return false;
        if (password.length() < 8) return false;
        boolean mayus = false, minus = false, dig = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) mayus = true;
            else if (Character.isLowerCase(c)) minus = true;
            else if (Character.isDigit(c)) dig = true;
            if (mayus && minus && dig) return true;
        }
        return false;
    }
}
