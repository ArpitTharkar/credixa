package com.arpit.myapplication.finance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/** Deterministic demo data so the app looks alive without importing a real statement. */
public final class SampleData {
    private SampleData() {}

    private static final String[][] SHOPS = {
            // name, category, min, max
            {"Swiggy", "Food & Dining", "150", "700"},
            {"Zomato", "Food & Dining", "120", "650"},
            {"Shreenath pay and park", "Transport", "20", "40"},
            {"Jyoti Petroleum", "Transport", "300", "1500"},
            {"Uber", "Transport", "80", "450"},
            {"BigBasket", "Groceries", "400", "2200"},
            {"Local Kirana Store", "Groceries", "60", "400"},
            {"Amazon", "Shopping", "300", "6000"},
            {"Myntra", "Shopping", "500", "3500"},
            {"Airtel Recharge", "Bills & Recharge", "299", "599"},
            {"Electricity Board", "Bills & Recharge", "800", "2400"},
            {"Netflix", "Entertainment", "199", "649"},
            {"BookMyShow", "Entertainment", "250", "900"},
            {"Apollo Pharmacy", "Health", "80", "1400"},
            {"Zerodha", "Investments", "1000", "9000"},
            {"Rahul Sharma", "Personal Transfer", "100", "1500"},
            {"Priya Patel", "Personal Transfer", "50", "900"},
            {"Chai Point", "Food & Dining", "20", "120"},
            {"MakeMyTrip", "Travel", "1800", "9500"},
    };

    private static final String[] PEOPLE = {"Piyush Tharkar", "Kaivalya Tolmare", "Diptesh Patil", "Rahul Sharma"};
    private static final String[] METHODS = {"GPay", "UPI", "Card", "GPay", "UPI"};

    public static List<FinTxn> generate(int months) {
        Random r = new Random(42);
        List<FinTxn> out = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        for (int m = months - 1; m >= 0; m--) {
            Calendar c = (Calendar) now.clone();
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, -m);
            int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            boolean current = m == 0;
            int lastDay = current ? now.get(Calendar.DAY_OF_MONTH) : days;

            // Salary on the 1st
            out.add(tx(c, 1, 9, 30, 30000, true, "Salary", "Salary", "Bank"));
            // Monthly rent-like transfer on the 10th
            if (lastDay >= 10) out.add(tx(c, 10, 11, 5, 3300 + r.nextInt(40), false, "Shubham Tharkar", "Personal Transfer", "GPay"));
            // Few incoming transfers
            for (int i = 0; i < 2 + r.nextInt(3); i++) {
                int d = 1 + r.nextInt(lastDay);
                out.add(tx(c, d, 10 + r.nextInt(10), r.nextInt(60), 200 + r.nextInt(2500), true,
                        PEOPLE[r.nextInt(PEOPLE.length)], "Other", "GPay"));
            }
            // Daily-ish spending
            for (int d = 1; d <= lastDay; d++) {
                int n = r.nextInt(100) < 85 ? 1 + r.nextInt(3) : 0;
                for (int k = 0; k < n; k++) {
                    String[] s = SHOPS[r.nextInt(SHOPS.length)];
                    int lo = Integer.parseInt(s[2]), hi = Integer.parseInt(s[3]);
                    double amount = lo + r.nextInt(Math.max(1, hi - lo)) + (r.nextInt(100) / 100.0);
                    if (r.nextInt(100) < 70) amount = Math.floor(amount);
                    int hour = 8 + (int) Math.min(15, Math.abs(r.nextGaussian() * 4) + 6);
                    FinTxn t = tx(c, d, Math.min(hour, 23), r.nextInt(60), amount, false, s[0], s[1], METHODS[r.nextInt(METHODS.length)]);
                    if (r.nextInt(100) < 3) t.status = "FAILED";
                    out.add(t);
                }
            }
        }
        for (FinTxn t : out) t.source = "sample";
        return out;
    }

    private static FinTxn tx(Calendar month, int day, int hour, int min, double amount, boolean received,
                             String who, String category, String method) {
        Calendar c = (Calendar) month.clone();
        c.set(Calendar.DAY_OF_MONTH, Math.min(day, c.getActualMaximum(Calendar.DAY_OF_MONTH)));
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        FinTxn t = new FinTxn();
        t.time = Math.min(c.getTimeInMillis(), System.currentTimeMillis());
        t.amount = amount;
        t.received = received;
        t.counterparty = who;
        t.category = category;
        t.method = method;
        return t;
    }
}
