package com.arpit.myapplication.finance;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Number and date formatting shared by all finance screens. */
public final class Fmt {
    private static final Locale IN = new Locale("en", "IN");

    private Fmt() {}

    private static String grouped(double v, int decimals) {
        NumberFormat nf = NumberFormat.getNumberInstance(IN);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        return nf.format(v);
    }

    /** ₹1,234.56 */
    public static String money(double v) { return "₹" + grouped(Math.abs(v), 2); }

    /** ₹45,163 */
    public static String whole(double v) { return "₹" + grouped(Math.abs(v), 0); }

    /** ₹30.0k, ₹1.2L, ₹510.00 */
    public static String compact(double v) {
        double a = Math.abs(v);
        if (a < 1000) return money(a);
        if (a < 100000) return String.format(Locale.ENGLISH, "₹%.1fk", a / 1000);
        if (a < 10000000) return String.format(Locale.ENGLISH, "₹%.1fL", a / 100000);
        return String.format(Locale.ENGLISH, "₹%.1fCr", a / 10000000);
    }

    public static String signed(double v, boolean received) {
        return (received ? "+" : "-") + money(v);
    }

    private static String fmt(String pattern, long t) {
        return new SimpleDateFormat(pattern, Locale.ENGLISH).format(new Date(t));
    }

    public static String monthLabel(long t) { return fmt("MMM yyyy", t); }

    public static String dayHeader(long t) { return fmt("dd MMM yyyy", t).toUpperCase(Locale.ENGLISH); }

    public static String shortDate(long t) { return fmt("d MMM yyyy", t); }

    public static String dateTime(long t) {
        return fmt("d MMM yyyy, h:mm a", t).replace("AM", "am").replace("PM", "pm");
    }

    public static String shortDateTime(long t) {
        return fmt("d MMM, h:mm a", t).replace("AM", "am").replace("PM", "pm");
    }

    public static String hour(int h) {
        int x = h % 12 == 0 ? 12 : h % 12;
        return x + (h < 12 ? "am" : "pm");
    }

    public static String hourRange(int h) { return hour(h) + "–" + hour((h + 1) % 24); }
}
