package com.arpit.myapplication.finance;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns CSV / JSON / Google Takeout HTML / PDF statements into {@link FinTxn}s, fully on-device. */
public final class ImportParsers {
    private ImportParsers() {}

    public static class ParseException extends Exception {
        public ParseException(String m) { super(m); }
    }

    // ---- shared helpers ----

    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd/MM/yyyy hh:mm a", "dd/MM/yyyy",
            "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm", "dd-MM-yyyy",
            "d MMM yyyy, HH:mm:ss", "d MMM yyyy, hh:mm:ss a", "d MMM yyyy, h:mm a", "d MMM yyyy HH:mm", "d MMM yyyy",
            "dd MMM yyyy", "MMM d, yyyy, h:mm:ss a", "MMM d, yyyy h:mm a", "MMM d, yyyy", "MMM dd, yyyy",
            "d MMMM yyyy", "MMMM d, yyyy"
    };

    public static long parseDate(String raw) {
        if (raw == null) return -1;
        String s = raw.trim().replace(" IST", "").replace(" ", " ").replace(" ", " ");
        if (s.isEmpty()) return -1;
        if (s.matches("^\\d{13}$")) return Long.parseLong(s);
        if (s.matches("^\\d{10}$")) return Long.parseLong(s) * 1000;
        for (String p : DATE_PATTERNS) {
            SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
            f.setLenient(false);
            ParsePosition pos = new ParsePosition(0);
            Date d = f.parse(s, pos);
            if (d != null && pos.getIndex() == s.length()) return d.getTime();
        }
        return -1;
    }

    private static double parseAmount(String raw) {
        if (raw == null) return Double.NaN;
        String cleaned = raw.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) return Double.NaN;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static boolean looksReceived(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("received") || l.contains("credit") || l.contains("incoming")
                || l.equals("cr") || l.equals("in") || l.contains("refund") || l.contains("deposit");
    }

    private static boolean looksFailed(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("fail") || l.contains("declin") || l.contains("cancel") || l.contains("reject");
    }

    private static FinTxn make(long time, double amount, boolean received, String who, String category,
                               String note, String method, boolean failed) {
        FinTxn t = new FinTxn();
        t.time = time;
        t.amount = Math.abs(amount);
        t.received = received;
        t.counterparty = who == null || who.trim().isEmpty() ? "Unknown" : who.trim();
        t.note = note == null ? "" : note.trim();
        t.method = method == null ? "" : method.trim();
        t.category = category == null || category.trim().isEmpty()
                ? Categorizer.categorize(t.counterparty, t.note, received) : category.trim();
        t.status = failed ? "FAILED" : "SUCCESS";
        t.source = "import";
        return t;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        String s = new String(out.toByteArray(), StandardCharsets.UTF_8);
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    // ---- field aliases for CSV / JSON ----

    private static final String[] TIME_KEYS = {"timestamp", "date", "time", "datetime", "created", "createdat", "transaction date"};
    private static final String[] ACTION_KEYS = {"action", "type", "direction", "transaction type", "dr/cr", "drcr"};
    private static final String[] AMOUNT_KEYS = {"amount", "amount (inr)", "value", "amt", "inr"};
    private static final String[] WHO_KEYS = {"counterparty", "merchant", "name", "payee", "to", "from", "description", "details", "narration"};
    private static final String[] CAT_KEYS = {"category"};
    private static final String[] NOTE_KEYS = {"note", "notes", "remark", "remarks", "memo"};
    private static final String[] METHOD_KEYS = {"method", "payment method", "mode", "bank", "instrument", "app"};
    private static final String[] STATUS_KEYS = {"status", "state"};

    private static FinTxn fromFields(java.util.Map<String, String> row) throws ParseException {
        String timeRaw = first(row, TIME_KEYS);
        String amountRaw = first(row, AMOUNT_KEYS);
        if (timeRaw == null || amountRaw == null) return null;
        long time = parseDate(timeRaw);
        double amount = parseAmount(amountRaw);
        if (time < 0 || Double.isNaN(amount) || amount == 0) return null;
        String action = first(row, ACTION_KEYS);
        boolean received;
        if (action != null && !action.isEmpty()) received = looksReceived(action);
        else received = amount > 0 && amountRaw.trim().startsWith("+");
        if ((action == null || action.isEmpty()) && amount < 0) received = false;
        String status = first(row, STATUS_KEYS);
        boolean failed = status != null && looksFailed(status);
        return make(time, amount, received, first(row, WHO_KEYS), first(row, CAT_KEYS),
                first(row, NOTE_KEYS), first(row, METHOD_KEYS), failed);
    }

    private static String first(java.util.Map<String, String> row, String[] keys) {
        for (String k : keys) {
            String v = row.get(k);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    // ---- CSV ----

    public static List<FinTxn> parseCsv(InputStream in) throws Exception {
        String[] lines = readAll(in).split("\\r?\\n");
        int h = 0;
        while (h < lines.length && lines[h].trim().isEmpty()) h++;
        if (h >= lines.length) throw new ParseException("The file is empty.");
        List<String> header = splitCsv(lines[h]);
        for (int i = 0; i < header.size(); i++) header.set(i, header.get(i).trim().toLowerCase(Locale.ROOT));
        if (!hasAny(header, AMOUNT_KEYS) || !hasAny(header, TIME_KEYS)) {
            throw new ParseException("CSV needs a date/timestamp column and an amount column.");
        }
        List<FinTxn> out = new ArrayList<>();
        for (int i = h + 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            List<String> cells = splitCsv(lines[i]);
            java.util.Map<String, String> row = new java.util.HashMap<>();
            for (int c = 0; c < header.size() && c < cells.size(); c++) row.put(header.get(c), cells.get(c));
            FinTxn t = fromFields(row);
            if (t != null) out.add(t);
        }
        return out;
    }

    private static boolean hasAny(List<String> header, String[] keys) {
        for (String k : keys) if (header.contains(k)) return true;
        return false;
    }

    private static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (q && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else q = !q;
            } else if (ch == ',' && !q) {
                out.add(sb.toString());
                sb.setLength(0);
            } else sb.append(ch);
        }
        out.add(sb.toString());
        return out;
    }

    // ---- JSON ----

    public static List<FinTxn> parseJson(InputStream in) throws Exception {
        String text = readAll(in).trim();
        JSONArray arr;
        if (text.startsWith("[")) arr = new JSONArray(text);
        else {
            JSONObject o = new JSONObject(text);
            arr = o.optJSONArray("transactions");
            if (arr == null) arr = o.optJSONArray("data");
            if (arr == null) throw new ParseException("JSON must be an array of transactions.");
        }
        List<FinTxn> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            java.util.Map<String, String> row = new java.util.HashMap<>();
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                row.put(k.toLowerCase(Locale.ROOT), String.valueOf(o.opt(k)));
            }
            FinTxn t = fromFields(row);
            if (t != null) out.add(t);
        }
        return out;
    }

    // ---- Google Takeout "My Activity.html" ----

    private static final Pattern TAKEOUT_ACTION = Pattern.compile(
            "^(Paid|Sent|Received|Declined|Failed|Cancelled)\\s*(?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:to|from|using)?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TAKEOUT_DATE = Pattern.compile(
            "(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4},\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[APap][Mm])?)");

    public static List<FinTxn> parseTakeout(InputStream in) throws Exception {
        String html = readAll(in)
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(div|p|li|tr|h\\d)>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&#8377;", "₹").replace("&amp;", "&")
                .replace(" ", " ").replace(" ", " ");
        String[] lines = html.split("\\r?\\n");
        List<FinTxn> out = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            Matcher m = TAKEOUT_ACTION.matcher(lines[i].trim().replaceAll("\\s+", " "));
            if (!m.find()) continue;
            // The timestamp sits on the same or one of the next few lines.
            long time = -1;
            for (int j = i; j < Math.min(lines.length, i + 4) && time < 0; j++) {
                Matcher d = TAKEOUT_DATE.matcher(lines[j]);
                if (d.find()) time = parseDate(d.group(1).replaceAll("\\s+", " "));
            }
            if (time < 0) continue;
            String verb = m.group(1).toLowerCase(Locale.ROOT);
            boolean received = verb.equals("received");
            boolean failed = !(verb.equals("paid") || verb.equals("sent") || received);
            String who = m.group(3).replaceAll(TAKEOUT_DATE.pattern(), "").trim();
            out.add(make(time, parseAmount(m.group(2)), received, who, null, "", "GPay", failed));
        }
        if (out.isEmpty()) throw new ParseException("No Google Pay activity found in this file.");
        return out;
    }

    // ---- PDF statements (PhonePe / GPay / bank style lines) ----

    private static final Pattern PDF_DATE = Pattern.compile(
            "(\\d{1,2}\\s+[A-Za-z]{3,9},?\\s+\\d{4}|[A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{4}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})");
    private static final Pattern PDF_AMOUNT = Pattern.compile("(?:₹|INR|Rs\\.?)\\s*([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern PDF_TIME = Pattern.compile("(\\d{1,2}:\\d{2}\\s?(?:[APap][Mm])?)");

    public static List<FinTxn> parsePdf(Context context, InputStream in) throws Exception {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.getApplicationContext());
        String text;
        try (com.tom_roush.pdfbox.pdmodel.PDDocument doc = com.tom_roush.pdfbox.pdmodel.PDDocument.load(in)) {
            text = new com.tom_roush.pdfbox.text.PDFTextStripper().getText(doc);
        }
        String[] lines = text.split("\\r?\\n");
        List<FinTxn> out = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().replace(" ", " ");
            Matcher dm = PDF_DATE.matcher(line);
            Matcher am = PDF_AMOUNT.matcher(line);
            if (!dm.find() || !am.find()) continue;
            String dateStr = dm.group(1).replace(",", "");
            String timeStr = "";
            Matcher tm = PDF_TIME.matcher(line);
            if (tm.find()) timeStr = tm.group(1);
            else if (i + 1 < lines.length) {
                Matcher tn = PDF_TIME.matcher(lines[i + 1]);
                if (tn.find()) timeStr = tn.group(1);
            }
            long time = parseDate(dateStr + (timeStr.isEmpty() ? "" : " " + timeStr));
            if (time < 0) time = parseDate(dateStr);
            if (time < 0) {
                time = parseDate(dateStr.replaceAll("(\\w{3})\\w* (\\d{1,2}) (\\d{4})", "$2 $1 $3"));
            }
            if (time < 0) continue;
            double amount = parseAmount(am.group(1));
            if (Double.isNaN(amount) || amount == 0) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            boolean received = lower.contains("credit") || lower.contains("received from");
            String who = line.replace(dm.group(1), " ").replaceAll(PDF_AMOUNT.pattern(), " ")
                    .replaceAll("(?i)\\b(debit|credit|paid to|received from|transaction id|utr no)\\b.*$", " ")
                    .replaceAll(PDF_TIME.pattern(), " ").replaceAll("\\s+", " ").trim();
            if (who.isEmpty()) who = "Statement entry";
            out.add(make(time, amount, received, who, null, "", "PDF", false));
        }
        if (out.isEmpty()) throw new ParseException("Could not find transactions in this PDF.");
        return out;
    }
}
