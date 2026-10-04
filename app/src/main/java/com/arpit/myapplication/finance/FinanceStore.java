package com.arpit.myapplication.finance;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * On-device storage for transactions, budgets, goals and holdings of the logged in user.
 * Everything lives in one JSON file; nothing is sent to a server.
 */
public class FinanceStore {

    public static class ImportRec {
        public String id;
        public String name;
        public long time;
        public int count;
    }

    public static class Goal {
        public String id = UUID.randomUUID().toString();
        public String name;
        public double target;
        public long startMs;
        public long deadlineMs;
    }

    public static class Holding {
        public String id = UUID.randomUUID().toString();
        public String name;
        public String type;
        public double invested;
        public double current;
    }

    private static FinanceStore instance;
    private static String instanceUser;

    private final File file;
    private final List<FinTxn> txns = new ArrayList<>();
    private final List<ImportRec> imports = new ArrayList<>();
    private final Map<String, Double> budgets = new HashMap<>();
    private final List<Goal> goals = new ArrayList<>();
    private final List<Holding> holdings = new ArrayList<>();

    public static synchronized FinanceStore get(Context context) {
        String user = com.arpit.myapplication.ServiceLocator.provideUserRepository().getCurrentUser();
        if (user == null) user = "guest";
        if (instance == null || !user.equals(instanceUser)) {
            String safe = user.replaceAll("[^A-Za-z0-9]", "_");
            instance = new FinanceStore(new File(context.getApplicationContext().getFilesDir(), "finance_" + safe + ".json"));
            instanceUser = user;
        }
        return instance;
    }

    private FinanceStore(File file) {
        this.file = file;
        load();
    }

    // ---- transactions ----

    public List<FinTxn> txns() { return txns; }

    public int count() { return txns.size(); }

    /** Adds transactions that are not already present. Returns how many were added. */
    public int addAll(List<FinTxn> incoming, ImportRec rec) {
        Set<String> seen = new HashSet<>();
        for (FinTxn t : txns) seen.add(t.dedupeKey());
        int added = 0;
        for (FinTxn t : incoming) {
            if (seen.add(t.dedupeKey())) {
                if (rec != null) t.importId = rec.id;
                txns.add(t);
                added++;
            }
        }
        if (rec != null && added > 0) {
            rec.count = added;
            imports.add(0, rec);
        }
        sortAndSave();
        return added;
    }

    public void addManual(FinTxn t) {
        t.source = "manual";
        txns.add(t);
        sortAndSave();
    }

    public void remove(Set<String> ids) {
        for (Iterator<FinTxn> it = txns.iterator(); it.hasNext(); ) {
            if (ids.contains(it.next().id)) it.remove();
        }
        save();
    }

    public void setCategory(Set<String> ids, String category) {
        for (FinTxn t : txns) if (ids.contains(t.id)) t.category = category;
        save();
    }

    /** Replaces everything previously synced from the wallet server. */
    public void replaceWalletTxns(List<FinTxn> wallet) {
        for (Iterator<FinTxn> it = txns.iterator(); it.hasNext(); ) {
            if ("wallet".equals(it.next().source)) it.remove();
        }
        txns.addAll(wallet);
        sortAndSave();
    }

    public void deleteAll() {
        txns.clear();
        imports.clear();
        save();
    }

    // ---- imports ----

    public List<ImportRec> imports() { return imports; }

    public void removeImport(String importId) {
        for (Iterator<FinTxn> it = txns.iterator(); it.hasNext(); ) {
            if (importId.equals(it.next().importId)) it.remove();
        }
        for (Iterator<ImportRec> it = imports.iterator(); it.hasNext(); ) {
            if (it.next().id.equals(importId)) it.remove();
        }
        save();
    }

    // ---- budgets ----

    /** key = "overall" or a category name. month = "yyyy-MM" or null for the default. */
    public double budget(String key, String month) {
        if (month != null) {
            Double v = budgets.get(key + "|" + month);
            if (v != null) return v;
        }
        Double d = budgets.get(key + "|default");
        return d == null ? 0 : d;
    }

    public boolean hasMonthOverride(String key, String month) { return budgets.containsKey(key + "|" + month); }

    public void setBudget(String key, String monthOrNull, double amount) {
        String k = key + "|" + (monthOrNull == null ? "default" : monthOrNull);
        if (amount <= 0) budgets.remove(k);
        else budgets.put(k, amount);
        save();
    }

    // ---- goals / holdings ----

    public List<Goal> goals() { return goals; }

    public void saveGoal(Goal g) {
        if (!goals.contains(g)) goals.add(g);
        save();
    }

    public void deleteGoal(Goal g) {
        goals.remove(g);
        save();
    }

    public List<Holding> holdings() { return holdings; }

    public void saveHolding(Holding h) {
        if (!holdings.contains(h)) holdings.add(h);
        save();
    }

    public void deleteHolding(Holding h) {
        holdings.remove(h);
        save();
    }

    // ---- persistence ----

    private void sortAndSave() {
        Collections.sort(txns, (a, b) -> Long.compare(b.time, a.time));
        save();
    }

    private void load() {
        if (!file.exists()) return;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            JSONObject root = new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8));
            JSONArray a = root.optJSONArray("txns");
            if (a != null) for (int i = 0; i < a.length(); i++) txns.add(FinTxn.fromJson(a.getJSONObject(i)));
            a = root.optJSONArray("imports");
            if (a != null) for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                ImportRec r = new ImportRec();
                r.id = o.optString("id");
                r.name = o.optString("name");
                r.time = o.optLong("time");
                r.count = o.optInt("count");
                imports.add(r);
            }
            JSONObject b = root.optJSONObject("budgets");
            if (b != null) for (Iterator<String> it = b.keys(); it.hasNext(); ) {
                String k = it.next();
                budgets.put(k, b.optDouble(k));
            }
            a = root.optJSONArray("goals");
            if (a != null) for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Goal g = new Goal();
                g.id = o.optString("id", g.id);
                g.name = o.optString("name");
                g.target = o.optDouble("target");
                g.startMs = o.optLong("startMs");
                g.deadlineMs = o.optLong("deadlineMs");
                goals.add(g);
            }
            a = root.optJSONArray("holdings");
            if (a != null) for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Holding h = new Holding();
                h.id = o.optString("id", h.id);
                h.name = o.optString("name");
                h.type = o.optString("type");
                h.invested = o.optDouble("invested");
                h.current = o.optDouble("current");
                holdings.add(h);
            }
            Collections.sort(txns, (x, y) -> Long.compare(y.time, x.time));
        } catch (Exception e) {
            // Corrupt file: start empty rather than crash.
            txns.clear();
        }
    }

    private void save() {
        try {
            JSONObject root = new JSONObject();
            JSONArray a = new JSONArray();
            for (FinTxn t : txns) a.put(t.toJson());
            root.put("txns", a);
            a = new JSONArray();
            for (ImportRec r : imports) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("name", r.name);
                o.put("time", r.time);
                o.put("count", r.count);
                a.put(o);
            }
            root.put("imports", a);
            JSONObject b = new JSONObject();
            for (Map.Entry<String, Double> e : budgets.entrySet()) b.put(e.getKey(), e.getValue());
            root.put("budgets", b);
            a = new JSONArray();
            for (Goal g : goals) {
                JSONObject o = new JSONObject();
                o.put("id", g.id);
                o.put("name", g.name);
                o.put("target", g.target);
                o.put("startMs", g.startMs);
                o.put("deadlineMs", g.deadlineMs);
                a.put(o);
            }
            root.put("goals", a);
            a = new JSONArray();
            for (Holding h : holdings) {
                JSONObject o = new JSONObject();
                o.put("id", h.id);
                o.put("name", h.name);
                o.put("type", h.type);
                o.put("invested", h.invested);
                o.put("current", h.current);
                a.put(o);
            }
            root.put("holdings", a);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // Storage failure should not crash the UI.
        }
    }
}
