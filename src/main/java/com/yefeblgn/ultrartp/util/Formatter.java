package com.yefeblgn.ultrartp.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Sayı ve süre biçimlendirme.
 */
public final class Formatter {

    private static final DecimalFormat MONEY;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        MONEY = new DecimalFormat("#,##0.##", symbols);
    }

    private Formatter() {
    }

    public static String money(double amount) {
        return MONEY.format(amount);
    }

    public static String number(double value) {
        return MONEY.format(value);
    }

    /**
     * Saniyeyi "1sa 5dk 30sn" gibi okunur hâle getirir.
     */
    public static String time(long seconds) {
        if (seconds <= 0) return "0sn";

        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("g ");
        if (hours > 0) sb.append(hours).append("sa ");
        if (minutes > 0) sb.append(minutes).append("dk ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("sn");

        return sb.toString().trim();
    }

    /**
     * İlerleme çubuğu üretir.
     */
    public static String progressBar(double progress, int length, String filled, String empty) {
        double clamped = Math.max(0.0D, Math.min(1.0D, progress));
        int done = (int) Math.round(clamped * length);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(i < done ? filled : empty);
        }
        return sb.toString();
    }

    public static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase(Locale.ROOT) + input.substring(1);
    }
}
