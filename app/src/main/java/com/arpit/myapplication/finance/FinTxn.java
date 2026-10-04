package com.arpit.myapplication.finance;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;

/** One money movement (sent or received) used by all finance screens. */
public class FinTxn {
    public String id = UUID.randomUUID().toString();
    public long time;
    public double amount;          // always positive
    public boolean received;
    public String counterparty = "";
    public String category = "Other";
    public String note = "";
    public String method = "";
    public String status = "SUCCESS";
    public String importId = "";
    public String source = "manual"; // manual | import | wallet | sample

    public boolean isFailed() { return "FAILED".equalsIgnoreCase(status); }

    public boolean isSpend() { return !received && !isFailed(); }

    public boolean isIncome() { return received && !isFailed(); }

    public String dedupeKey() {
        return (time / 1000) + "|" + Math.round(amount * 100) + "|" + received + "|"
                + counterparty.toLowerCase(Locale.ROOT);
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("time", time);
            o.put("amount", amount);
            o.put("received", received);
            o.put("counterparty", counterparty);
            o.put("category", category);
            o.put("note", note);
            o.put("method", method);
            o.put("status", status);
            o.put("importId", importId);
            o.put("source", source);
        } catch (JSONException ignored) {
        }
        return o;
    }

    public static FinTxn fromJson(JSONObject o) {
        FinTxn t = new FinTxn();
        t.id = o.optString("id", t.id);
        t.time = o.optLong("time");
        t.amount = o.optDouble("amount");
        t.received = o.optBoolean("received");
        t.counterparty = o.optString("counterparty", "");
        t.category = o.optString("category", "Other");
        t.note = o.optString("note", "");
        t.method = o.optString("method", "");
        t.status = o.optString("status", "SUCCESS");
        t.importId = o.optString("importId", "");
        t.source = o.optString("source", "manual");
        return t;
    }
}
