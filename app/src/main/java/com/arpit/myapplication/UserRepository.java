package com.arpit.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

public class UserRepository {
    private static final String PREFS = "users_prefs";
    private static final String KEY_BACKEND_USER_ID = "backend_user_id";
    private static final String KEY_SESSION_TOKEN = "session_token";
    private final SharedPreferences prefs;

    public UserRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void addUser(String username, String phone) {
        Set<String> users = getUsers();
        users.add(username);
        prefs.edit().putStringSet("users", users).apply();
        prefs.edit().putString("phone_" + username, phone == null ? "" : phone).apply();
    }

    public boolean exists(String username) {
        return getUsers().contains(username);
    }

    private Set<String> getUsers() {
        Set<String> s = prefs.getStringSet("users", null);
        if (s == null) return new HashSet<>();
        return new HashSet<>(s);
    }

    public String getPhone(String username) { return prefs.getString("phone_" + username, ""); }

    public void setCurrentUser(String username) { prefs.edit().putString("current_user", username).apply(); }

    public String getCurrentUser() { return prefs.getString("current_user", null); }

    public void setBackendUserId(Long userId) {
        if (userId == null) {
            prefs.edit().remove(KEY_BACKEND_USER_ID).apply();
            return;
        }
        prefs.edit().putLong(KEY_BACKEND_USER_ID, userId).apply();
    }

    public Long getBackendUserId() {
        if (!prefs.contains(KEY_BACKEND_USER_ID)) return null;
        return prefs.getLong(KEY_BACKEND_USER_ID, -1L);
    }

    public void setSessionToken(String token) {
        if (token == null) prefs.edit().remove(KEY_SESSION_TOKEN).apply();
        else prefs.edit().putString(KEY_SESSION_TOKEN, token).apply();
    }

    public String getSessionToken() {
        return prefs.getString(KEY_SESSION_TOKEN, null);
    }

    public String findUserByPhone(String phone) {
        if (phone == null || phone.isEmpty()) return null;
        Set<String> users = getUsers();
        for (String u : users) {
            String p = getPhone(u);
            if (phone.equals(p)) return u;
        }
        return null;
    }
}
