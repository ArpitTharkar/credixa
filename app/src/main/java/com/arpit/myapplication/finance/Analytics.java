package com.arpit.myapplication.finance;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure calculations behind the dashboard, insights and planner screens. */
public final class Analytics {
    private Analytics() {}

    public static final long DAY = 24L * 60 * 60 * 1000;

    // ---- basic totals ----

    public static class Totals {
        public double spent, received;
        public int sentCount, receivedCount, count;
        public double net() { return received - spent; }
    }

    public static List<FinTxn> filter(List<FinTxn> all, Period p) {
        List<FinTxn> out = new ArrayList<>();
        for (FinTxn t : all) if (p.contains(t.time)) out.add(t);
        return out;
    }

    public static Totals totals(List<FinTxn> all, Period p) {
        Totals r = new Totals();
        for (FinTxn t : all) {
            if (!p.contains(t.time) || t.isFailed()) continue;
            r.count++;
            if (t.received) {
                r.received += t.amount;
                r.receivedCount++;
            } else {
                r.spent += t.amount;
                r.sentCount++;
            }
        }
        return r;
    }

    public static long latestTime(List<FinTxn> all) {
        long m = 0;
        for (FinTxn t : all) m = Math.max(m, t.time);
        return m;
    }

    // ---- monthly trend ----

    public static class Trend {
        public String[] labels = new String[0];
        public double[] spent = new double[0];
        public double[] received = new double[0];
    }

    public static Trend monthlyTrend(List<FinTxn> all, Period anchor) {
        Period end = anchor.all ? (all.isEmpty() ? Period.now() : Period.ofTime(latestTime(all))) : anchor;
        long first = Long.MAX_VALUE;
        for (FinTxn t : all) first = Math.min(first, t.time);
        Period firstP = first == Long.MAX_VALUE ? end : Period.ofTime(first);
        int months = (end.year - firstP.year) * 12 + (end.month - firstP.month) + 1;
        months = Math.max(1, Math.min(12, months));
        Trend tr = new Trend();
        tr.labels = new String[months];
        tr.spent = new double[months];
        tr.received = new double[months];
        for (int i = 0; i < months; i++) {
            Period p = end.shift(-(months - 1 - i));
            tr.labels[i] = Fmt.monthLabel(p.start()).substring(0, 3);
            Totals t = totals(all, p);
            tr.spent[i] = t.spent;
            tr.received[i] = t.received;
        }
        return tr;
    }

    // ---- grouping helpers ----

    public static class Entry {
        public String name;
        public int count;
        public double total;
        public long lastTime;
    }

    public static List<Entry> group(List<FinTxn> all, Period p, boolean received, boolean byMethod) {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (FinTxn t : all) {
            if (!p.contains(t.time) || t.isFailed() || t.received != received) continue;
            String name = byMethod ? (t.method.isEmpty() ? "(unknown)" : t.method) : t.counterparty;
            Entry e = map.get(name);
            if (e == null) {
                e = new Entry();
                e.name = name;
                map.put(name, e);
            }
            e.count++;
            e.total += t.amount;
            e.lastTime = Math.max(e.lastTime, t.time);
        }
        List<Entry> list = new ArrayList<>(map.values());
        Collections.sort(list, (a, b) -> Double.compare(b.total, a.total));
        return list;
    }

    public static Map<String, Double> categorySpend(List<FinTxn> all, Period p) {
        Map<String, Double> m = new HashMap<>();
        for (FinTxn t : all) {
            if (!p.contains(t.time) || !t.isSpend()) continue;
            Double v = m.get(t.category);
            m.put(t.category, (v == null ? 0 : v) + t.amount);
        }
        return m;
    }

    /** Average monthly spend in a category across months that have any data. */
    public static double averageMonthlyCategorySpend(List<FinTxn> all, String category) {
        double sum = 0;
        Set<String> months = new HashSet<>();
        for (FinTxn t : all) {
            months.add(Period.ofTime(t.time).key());
            if (t.isSpend() && t.category.equals(category)) sum += t.amount;
        }
        return months.isEmpty() ? 0 : sum / months.size();
    }

    // ---- time-of-day patterns ----

    public static double[] hourSpend(List<FinTxn> all, Period p) {
        double[] h = new double[24];
        Calendar c = Calendar.getInstance();
        for (FinTxn t : all) {
            if (!p.contains(t.time) || !t.isSpend()) continue;
            c.setTimeInMillis(t.time);
            h[c.get(Calendar.HOUR_OF_DAY)] += t.amount;
        }
        return h;
    }

    public static class Heat {
        public double[][] amount = new double[7][24]; // row 0 = Monday
        public int[][] count = new int[7][24];
    }

    public static Heat heat(List<FinTxn> all, Period p) {
        Heat h = new Heat();
        Calendar c = Calendar.getInstance();
        for (FinTxn t : all) {
            if (!p.contains(t.time) || !t.isSpend()) continue;
            c.setTimeInMillis(t.time);
            int day = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Mon=0 .. Sun=6
            int hour = c.get(Calendar.HOUR_OF_DAY);
            h.amount[day][hour] += t.amount;
            h.count[day][hour]++;
        }
        return h;
    }

    public static class Weekly {
        public long[] weekStart = new long[0];
        public double[] total = new double[0];
    }

    public static Weekly weekly(List<FinTxn> all, Period p, int weeks) {
        long ref = p.all ? Math.min(System.currentTimeMillis(), Math.max(latestTime(all), 1))
                : Math.min(System.currentTimeMillis(), p.end() - 1);
        if (all.isEmpty()) ref = System.currentTimeMillis();
        long lastWeek = mondayStart(ref);
        Weekly w = new Weekly();
        w.weekStart = new long[weeks];
        w.total = new double[weeks];
        for (int i = 0; i < weeks; i++) w.weekStart[i] = lastWeek - (long) (weeks - 1 - i) * 7 * DAY;
        for (FinTxn t : all) {
            if (!t.isSpend()) continue;
            if (!p.all && !p.contains(t.time)) continue;
            long idx = (t.time - w.weekStart[0]) / (7 * DAY);
            if (t.time >= w.weekStart[0] && idx >= 0 && idx < weeks) w.total[(int) idx] += t.amount;
        }
        return w;
    }

    private static long mondayStart(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        int back = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -back);
        return c.getTimeInMillis();
    }

    public static final String[] SIZE_LABELS = {"Under ₹100", "₹100 – 500", "₹500 – 2k", "₹2k – 10k", "Over ₹10k"};

    public static class Sizes {
        public int[] count = new int[5];
        public double[] sum = new double[5];
    }

    public static Sizes sizes(List<FinTxn> all, Period p) {
        Sizes s = new Sizes();
        for (FinTxn t : all) {
            if (!p.contains(t.time) || !t.isSpend()) continue;
            int b = t.amount < 100 ? 0 : t.amount < 500 ? 1 : t.amount < 2000 ? 2 : t.amount < 10000 ? 3 : 4;
            s.count[b]++;
            s.sum[b] += t.amount;
        }
        return s;
    }

    // ---- behaviour ----

    public static class Behaviour {
        public double average;
        public FinTxn biggest;
        public double weekday, weekend;
        public int activeDays;
        public int newMerchants;
    }

    public static Behaviour behaviour(List<FinTxn> all, Period p) {
        Behaviour b = new Behaviour();
        int n = 0;
        double sum = 0;
        Set<String> days = new HashSet<>();
        Calendar c = Calendar.getInstance();
        Map<String, Long> firstSeen = new HashMap<>();
        for (FinTxn t : all) {
            if (!t.isSpend()) continue;
            String k = t.counterparty.toLowerCase(Locale.ROOT);
            Long f = firstSeen.get(k);
            if (f == null || t.time < f) firstSeen.put(k, t.time);
        }
        Set<String> newOnes = new HashSet<>();
        for (FinTxn t : all) {
            if (!p.contains(t.time) || !t.isSpend()) continue;
            n++;
            sum += t.amount;
            if (b.biggest == null || t.amount > b.biggest.amount) b.biggest = t;
            c.setTimeInMillis(t.time);
            days.add(c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR));
            int dow = c.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) b.weekend += t.amount;
            else b.weekday += t.amount;
            String k = t.counterparty.toLowerCase(Locale.ROOT);
            if (p.contains(firstSeen.get(k))) newOnes.add(k);
        }
        b.average = n == 0 ? 0 : sum / n;
        b.activeDays = days.size();
        b.newMerchants = newOnes.size();
        return b;
    }

    // ---- recurring payments ----

    public static class Recurring {
        public String name;
        public double amount;     // typical amount
        public int dayOfMonth;
        public int everyDays;
        public int times;
        public double paidThisMonth;
        public boolean paid;
        public long nextExpected;
    }

    public static List<Recurring> recurring(List<FinTxn> all) {
        Map<String, List<FinTxn>> byName = new HashMap<>();
        for (FinTxn t : all) {
            if (!t.isSpend()) continue;
            String k = t.counterparty.toLowerCase(Locale.ROOT);
            List<FinTxn> l = byName.get(k);
            if (l == null) {
                l = new ArrayList<>();
                byName.put(k, l);
            }
            l.add(t);
        }
        Period now = Period.now();
        List<Recurring> out = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        for (List<FinTxn> l : byName.values()) {
            if (l.size() < 3) continue;
            Collections.sort(l, (a, b) -> Long.compare(a.time, b.time));
            List<Long> gaps = new ArrayList<>();
            List<Double> amounts = new ArrayList<>();
            double mean = 0;
            for (int i = 0; i < l.size(); i++) {
                amounts.add(l.get(i).amount);
                mean += l.get(i).amount;
                if (i > 0) gaps.add(Math.round((l.get(i).time - l.get(i - 1).time) / (double) DAY));
            }
            mean /= l.size();
            Collections.sort(gaps);
            long medianGap = gaps.get(gaps.size() / 2);
            if (medianGap < 20 || medianGap > 40) continue;
            double var = 0;
            for (double a : amounts) var += (a - mean) * (a - mean);
            double sd = Math.sqrt(var / l.size());
            if (mean <= 0 || sd / mean > 0.35) continue;

            Recurring r = new Recurring();
            r.name = l.get(l.size() - 1).counterparty;
            r.amount = mean;
            r.everyDays = (int) medianGap;
            r.times = l.size();
            List<Integer> doms = new ArrayList<>();
            for (FinTxn t : l) {
                c.setTimeInMillis(t.time);
                doms.add(c.get(Calendar.DAY_OF_MONTH));
                if (now.contains(t.time)) r.paidThisMonth += t.amount;
            }
            Collections.sort(doms);
            r.dayOfMonth = doms.get(doms.size() / 2);
            r.paid = r.paidThisMonth > 0;
            r.nextExpected = l.get(l.size() - 1).time + medianGap * DAY;
            out.add(r);
        }
        Collections.sort(out, (a, b) -> Double.compare(b.amount, a.amount));
        return out;
    }

    // ---- salary ----

    public static List<FinTxn> salaryCredits(List<FinTxn> all) {
        List<FinTxn> out = new ArrayList<>();
        for (FinTxn t : all) if (t.isIncome() && Categorizer.SALARY.equals(t.category)) out.add(t);
        return out;
    }
}
