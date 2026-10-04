package com.arpit.myapplication.finance;

import java.util.List;

/** Remembers the month each finance tab was last looking at while the app is open. */
public final class FinanceState {
    private FinanceState() {}

    private static Period dash, insights, txns, planner;

    private static Period latestMonth(List<FinTxn> all) {
        return all.isEmpty() ? Period.now() : Period.ofTime(Analytics.latestTime(all));
    }

    public static synchronized Period dash(List<FinTxn> all) {
        if (dash == null) dash = latestMonth(all);
        return dash;
    }

    public static synchronized Period insights() {
        if (insights == null) insights = Period.all();
        return insights;
    }

    public static synchronized Period txns(List<FinTxn> all) {
        if (txns == null) txns = latestMonth(all);
        return txns;
    }

    public static synchronized Period planner() {
        if (planner == null) planner = Period.now();
        return planner;
    }

    /** Call after data is imported or removed so tabs jump to a month that has data. */
    public static synchronized void reset() {
        dash = null;
        txns = null;
    }
}
