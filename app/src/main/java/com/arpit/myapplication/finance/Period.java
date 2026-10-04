package com.arpit.myapplication.finance;

import java.util.Calendar;
import java.util.Locale;

/** Either one calendar month or "all time". */
public class Period {
    public boolean all;
    public int year;
    public int month; // 0-11

    public static Period month(int year, int month) {
        Period p = new Period();
        p.year = year;
        p.month = month;
        return p;
    }

    public static Period ofTime(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return month(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
    }

    public static Period now() { return ofTime(System.currentTimeMillis()); }

    public static Period all() {
        Period p = new Period();
        p.all = true;
        return p;
    }

    public Period copy() {
        Period p = new Period();
        p.all = all;
        p.year = year;
        p.month = month;
        return p;
    }

    public Period shift(int delta) {
        if (all) return this;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, 1);
        c.add(Calendar.MONTH, delta);
        return month(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
    }

    public long start() {
        if (all) return Long.MIN_VALUE;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, 1, 0, 0, 0);
        return c.getTimeInMillis();
    }

    public long end() {
        if (all) return Long.MAX_VALUE;
        return shift(1).start();
    }

    public boolean contains(long t) { return all || (t >= start() && t < end()); }

    public boolean isCurrentMonth() {
        Period n = now();
        return !all && n.year == year && n.month == month;
    }

    public boolean isAfterNow() { return !all && start() > System.currentTimeMillis(); }

    public int daysInMonth() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public String label() { return all ? "All data" : Fmt.monthLabel(start()); }

    public String key() { return String.format(Locale.ENGLISH, "%04d-%02d", year, month + 1); }
}
