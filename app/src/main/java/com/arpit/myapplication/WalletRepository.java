package com.arpit.myapplication;

import android.content.Context;

import com.arpit.myapplication.remote.ApiClient;
import com.arpit.myapplication.remote.BackendApi;
import com.arpit.myapplication.remote.model.AddMoneyRequest;
import com.arpit.myapplication.remote.model.AuthBridgeRequest;
import com.arpit.myapplication.remote.model.AuthBridgeResponse;
import com.arpit.myapplication.remote.model.ResolveUserResponse;
import com.arpit.myapplication.remote.model.TransactionDto;
import com.arpit.myapplication.remote.model.TransferRequest;
import com.arpit.myapplication.remote.model.TransferResponse;
import com.arpit.myapplication.remote.model.WalletResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletRepository {
    private final BackendApi api;

    public WalletRepository(Context context) {
        this.api = ApiClient.backendApi();
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // --- SharedPreferences storage reused for debts/groups and offline helpers ---
    private static final String PREFS = "wallet_prefs";
    private final android.content.SharedPreferences prefs;

    private String txKey(String userId) { return "tx_" + userId; }
    private String balanceKey(String userId) { return "balance_" + userId; }

    public interface BalanceCallback { void onResult(boolean ok, long balance); }
    public interface SimpleCallback { void onResult(boolean ok, String message); }
    public interface TransactionsCallback { void onResult(boolean ok, List<Transaction> list); }
    public interface ResolveUserCallback { void onResult(boolean ok, Long userId, String message); }

    public void getBalanceAsync(String userId, BalanceCallback cb) {
        Call<WalletResponse> c = api.getWallet(userId);
        c.enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(Call<WalletResponse> call, Response<WalletResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long bal = response.body().getBalance().longValue();
                    cb.onResult(true, bal);
                } else cb.onResult(false, 0L);
            }

            @Override
            public void onFailure(Call<WalletResponse> call, Throwable t) {
                cb.onResult(false, 0L);
            }
        });
    }

    public void addMoneyAsync(String userId, long amount, SimpleCallback cb) {
        Call<WalletResponse> c = api.addMoney(userId, new AddMoneyRequest(BigDecimal.valueOf(amount)));
        c.enqueue(new Callback<WalletResponse>() {
            @Override
            public void onResponse(Call<WalletResponse> call, Response<WalletResponse> response) {
                if (response.isSuccessful() && response.body() != null) cb.onResult(true, "Added");
                else cb.onResult(false, "Server error");
            }

            @Override
            public void onFailure(Call<WalletResponse> call, Throwable t) { cb.onResult(false, t.getMessage()); }
        });
    }

    public void getTransactionsAsync(String userId, TransactionsCallback cb) {
        Call<List<TransactionDto>> c = api.transactions(userId);
        c.enqueue(new Callback<List<TransactionDto>>() {
            @Override
            public void onResponse(Call<List<TransactionDto>> call, Response<List<TransactionDto>> response) {
                if (!response.isSuccessful() || response.body() == null) { cb.onResult(false, null); return; }
                List<Transaction> list = new ArrayList<>();
                Long currentUserId = null;
                try { currentUserId = Long.valueOf(userId); } catch (NumberFormatException ignored) {}
                for (TransactionDto d : response.body()) {
                    String type;
                    if (d.direction != null && !d.direction.isEmpty()) {
                        type = d.direction;
                    } else if (d.senderUserId != null && d.receiverUserId != null && d.senderUserId.equals(d.receiverUserId)) {
                        type = "ADD";
                    } else if (currentUserId != null && currentUserId.equals(d.receiverUserId)) {
                        type = "RECEIVED";
                    } else {
                        type = "SEND";
                    }
                    long createdAtMs = System.currentTimeMillis();
                    if (d.createdAt != null && !d.createdAt.isEmpty()) {
                        try {
                            createdAtMs = Instant.parse(d.createdAt).toEpochMilli();
                        } catch (Exception ignored) {
                            // Keep fallback timestamp.
                        }
                    }
                    list.add(new Transaction(type, d.amount.longValue(), createdAtMs, null, d.status));
                }
                cb.onResult(true, list);
            }

            @Override
            public void onFailure(Call<List<TransactionDto>> call, Throwable t) { cb.onResult(false, null); }
        });
    }

    public void transferAsync(Long senderUserId, Long receiverUserId, long amount, SimpleCallback cb) {
        String idempotency = UUID.randomUUID().toString();
        // default: 3 attempts with exponential backoff on network failures
        attemptTransfer(senderUserId, receiverUserId, amount, idempotency, 3, cb);
    }

    public void resolveUserIdAsync(String identifier, ResolveUserCallback cb) {
        Call<ResolveUserResponse> c = api.resolveUser(identifier);
        c.enqueue(new Callback<ResolveUserResponse>() {
            @Override
            public void onResponse(Call<ResolveUserResponse> call, Response<ResolveUserResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUserId() != null) {
                    cb.onResult(true, response.body().getUserId(), "OK");
                    return;
                }
                if (response.code() == 404 && isLikelyPhone(identifier)) {
                    // If receiver does not exist yet, create-or-get by phone so transfers can work across devices.
                    createOrGetUserByPhone(identifier, cb);
                    return;
                }
                cb.onResult(false, null, extractErrorMessage(response, "User not found"));
            }

            @Override
            public void onFailure(Call<ResolveUserResponse> call, Throwable t) {
                cb.onResult(false, null, t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    private void createOrGetUserByPhone(String phoneInput, ResolveUserCallback cb) {
        String normalizedPhone = normalizePhone(phoneInput);
        Call<AuthBridgeResponse> c = api.phoneLogin(new AuthBridgeRequest(normalizedPhone));
        c.enqueue(new Callback<AuthBridgeResponse>() {
            @Override
            public void onResponse(Call<AuthBridgeResponse> call, Response<AuthBridgeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUserId() != null) {
                    cb.onResult(true, response.body().getUserId(), "OK");
                    return;
                }
                cb.onResult(false, null, extractErrorMessage(response, "Receiver not found"));
            }

            @Override
            public void onFailure(Call<AuthBridgeResponse> call, Throwable t) {
                cb.onResult(false, null, t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    private void attemptTransfer(Long senderUserId, Long receiverUserId, long amount, String idempotency, int attemptsLeft, SimpleCallback cb) {
        Call<TransferResponse> c = api.transfer(new TransferRequest(senderUserId, receiverUserId, BigDecimal.valueOf(amount), idempotency));
        c.enqueue(new Callback<TransferResponse>() {
            @Override
            public void onResponse(Call<TransferResponse> call, Response<TransferResponse> response) {
                if (response.isSuccessful() && response.body() != null) cb.onResult(true, "OK");
                else cb.onResult(false, extractErrorMessage(response, "Transfer failed"));
            }

            @Override
            public void onFailure(Call<TransferResponse> call, Throwable t) {
                boolean isNetwork = t instanceof java.io.IOException;
                if (isNetwork && attemptsLeft > 1) {
                    // schedule retry with exponential backoff on main looper
                    long delayMs = 500L * (long) Math.pow(2, 3 - attemptsLeft);
                    android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                    h.postDelayed(() -> attemptTransfer(senderUserId, receiverUserId, amount, idempotency, attemptsLeft - 1, cb), delayMs);
                } else {
                    cb.onResult(false, t.getMessage());
                }
            }
        });
    }

    private String extractErrorMessage(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                if (body != null && !body.isEmpty()) {
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    String error = json.optString("error");
                    if (error != null && !error.isEmpty()) {
                        return error;
                    }
                }
            }
        } catch (Exception ignored) {
            // Keep fallback message when parsing fails.
        }
        return fallback + " (HTTP " + response.code() + ")";
    }

    private boolean isLikelyPhone(String identifier) {
        if (identifier == null) return false;
        String digits = identifier.replaceAll("[^0-9]", "");
        return digits.length() >= 10;
    }

    private String normalizePhone(String input) {
        String digits = input == null ? "" : input.replaceAll("[^0-9+]", "").trim();
        if (digits.isEmpty()) return "";
        if (digits.matches("^[0-9]{10}$")) return "+91" + digits;
        if (digits.startsWith("+")) return digits;
        return "+" + digits;
    }

    // Reuse Transaction inner class from previous version for UI mapping
    public static class Transaction {
        public final String type; // ADD, SEND, RECEIVED
        public final long amount;
        public final long time;
        public final String category;
        public final String status;

        public Transaction(String type, long amount, long time, String category, String status) {
            this.type = type;
            this.amount = amount;
            this.time = time;
            this.category = category;
            this.status = status;
        }
    }

    // --- Transactions/local storage helpers (backwards-compatible synchronous access) ---
    private org.json.JSONArray getTransactionsArray(String userId) {
        String s = prefs.getString(txKey(userId), null);
        if (s == null) return new org.json.JSONArray();
        try { return new org.json.JSONArray(s); } catch (org.json.JSONException e) { return new org.json.JSONArray(); }
    }

    public java.util.List<Transaction> getTransactions(String userId) {
        org.json.JSONArray arr = getTransactionsArray(userId);
        java.util.List<Transaction> list = new java.util.ArrayList<>();
        for (int i = arr.length() - 1; i >= 0; i--) {
            try {
                org.json.JSONObject o = arr.getJSONObject(i);
                String type = o.optString("type");
                long amount = o.optLong("amount");
                long time = o.optLong("time");
                String category = o.optString("category", null);
                String status = o.optString("status", null);
                list.add(new Transaction(type, amount, time, category, status));
            } catch (org.json.JSONException ignored) {}
        }
        return list;
    }

    // --- Debt / Split group helpers (kept from previous local impl) ---
    private static final String DEBTS_KEY = "debts_all";
    private static final String GROUPS_KEY = "split_groups";

    public static class Debt {
        public final String id;
        public final String groupId;
        public final String creditor;
        public final String debtor;
        public final long amount;
        public final long time;
        public final String status;

        public Debt(String id, String groupId, String creditor, String debtor, long amount, long time, String status) {
            this.id = id; this.groupId = groupId; this.creditor = creditor; this.debtor = debtor; this.amount = amount; this.time = time; this.status = status;
        }
    }

    public static class SplitGroup {
        public final String id; public final String name; public final String creator; public final long time;
        public SplitGroup(String id, String name, String creator, long time) { this.id = id; this.name = name; this.creator = creator; this.time = time; }
    }

    private org.json.JSONArray getDebtsArray() {
        String s = prefs.getString(DEBTS_KEY, null);
        if (s == null) return new org.json.JSONArray();
        try { return new org.json.JSONArray(s); } catch (org.json.JSONException e) { return new org.json.JSONArray(); }
    }

    private void saveDebtsArray(org.json.JSONArray arr) { prefs.edit().putString(DEBTS_KEY, arr.toString()).apply(); }

    public String createDebt(String creditor, String debtor, long amount) { return createDebt(creditor, debtor, amount, "default"); }

    public String createDebt(String creditor, String debtor, long amount, String groupId) {
        try {
            org.json.JSONArray arr = getDebtsArray();
            String id = String.valueOf(System.currentTimeMillis()) + "_" + (int)(Math.random()*10000);
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("id", id);
            o.put("groupId", groupId != null ? groupId : "default");
            o.put("creditor", creditor);
            o.put("debtor", debtor);
            o.put("amount", amount);
            o.put("time", System.currentTimeMillis());
            o.put("status", "PENDING");
            arr.put(o);
            saveDebtsArray(arr);
            return id;
        } catch (org.json.JSONException e) { return null; }
    }

    public java.util.List<Debt> getDebtsForGroup(String groupId) {
        org.json.JSONArray arr = getDebtsArray();
        java.util.List<Debt> list = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try {
                org.json.JSONObject o = arr.getJSONObject(i);
                String debtGroupId = o.optString("groupId", "default");
                if (groupId.equals(debtGroupId)) {
                    list.add(new Debt(o.optString("id"), debtGroupId, o.optString("creditor"), o.optString("debtor"), o.optLong("amount"), o.optLong("time"), o.optString("status")));
                }
            } catch (org.json.JSONException ignored) {}
        }
        return list;
    }

    public java.util.List<SplitGroup> getAllGroups() {
        String s = prefs.getString(GROUPS_KEY, null);
        org.json.JSONArray arr = s == null ? new org.json.JSONArray() : (s.length()==0 ? new org.json.JSONArray() : new org.json.JSONArray());
        try { if (s != null) arr = new org.json.JSONArray(s); } catch (org.json.JSONException ignored) {}
        java.util.List<SplitGroup> list = new java.util.ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try { org.json.JSONObject o = arr.getJSONObject(i); list.add(new SplitGroup(o.optString("id"), o.optString("name"), o.optString("creator"), o.optLong("time"))); } catch (org.json.JSONException ignored) {}
        }
        return list;
    }

    public String createGroup(String name, String creator) {
        try {
            String id = "g_" + System.currentTimeMillis() + "_" + (int)(Math.random()*10000);
            org.json.JSONArray arr = new org.json.JSONArray(prefs.getString(GROUPS_KEY, "[]"));
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("id", id); o.put("name", name); o.put("creator", creator); o.put("time", System.currentTimeMillis());
            arr.put(o);
            prefs.edit().putString(GROUPS_KEY, arr.toString()).apply();
            return id;
        } catch (org.json.JSONException e) { return null; }
    }

    public boolean deleteGroup(String groupId, String requester) {
        try {
            String s = prefs.getString(GROUPS_KEY, "[]");
            org.json.JSONArray groups = new org.json.JSONArray(s);
            org.json.JSONArray updated = new org.json.JSONArray();
            boolean found = false;
            boolean allowed = false;
            for (int i = 0; i < groups.length(); i++) {
                org.json.JSONObject o = groups.getJSONObject(i);
                if (groupId.equals(o.optString("id"))) {
                    found = true;
                    if (requester != null && requester.equals(o.optString("creator"))) {
                        allowed = true;
                    } else {
                        updated.put(o);
                    }
                } else {
                    updated.put(o);
                }
            }
            if (!found || !allowed) return false;
            prefs.edit().putString(GROUPS_KEY, updated.toString()).apply();
            // remove debts in this group
            org.json.JSONArray debts = getDebtsArray();
            org.json.JSONArray updatedDebts = new org.json.JSONArray();
            for (int i = 0; i < debts.length(); i++) {
                org.json.JSONObject d = debts.getJSONObject(i);
                String debtGroupId = d.optString("groupId", "default");
                if (!groupId.equals(debtGroupId)) updatedDebts.put(d);
            }
            saveDebtsArray(updatedDebts);
            return true;
        } catch (org.json.JSONException ignored) { return false; }
    }

    public boolean markDebtPaid(String debtId) {
        try {
            org.json.JSONArray arr = getDebtsArray();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                if (debtId.equals(o.optString("id"))) {
                    o.put("status", "PAID");
                    prefs.edit().putString(DEBTS_KEY, arr.toString()).apply();
                    return true;
                }
            }
        } catch (org.json.JSONException ignored) {}
        return false;
    }

    public void clearDebtsForGroup(String groupId) {
        try {
            org.json.JSONArray debts = getDebtsArray();
            org.json.JSONArray updated = new org.json.JSONArray();
            for (int i = 0; i < debts.length(); i++) {
                org.json.JSONObject d = debts.getJSONObject(i);
                String debtGroupId = d.optString("groupId", "default");
                if (!groupId.equals(debtGroupId)) updated.put(d);
            }
            saveDebtsArray(updated);
        } catch (org.json.JSONException ignored) {}
    }

    // Category limits
    private String limitKey(String userId, String category) { return "limit_" + userId + "_" + (category != null ? category : "Other"); }
    public void setCategoryLimit(String userId, String category, long limit) { prefs.edit().putLong(limitKey(userId, category), Math.max(0L, limit)).apply(); }
    public long getCategoryLimit(String userId, String category) { return prefs.getLong(limitKey(userId, category), 0L); }
}
