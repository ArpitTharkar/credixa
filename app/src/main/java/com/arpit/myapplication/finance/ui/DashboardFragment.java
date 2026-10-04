package com.arpit.myapplication.finance.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arpit.myapplication.R;
import com.arpit.myapplication.finance.Analytics;
import com.arpit.myapplication.finance.FinTxn;
import com.arpit.myapplication.finance.FinanceState;
import com.arpit.myapplication.finance.Fmt;
import com.arpit.myapplication.finance.Period;

import java.util.List;

public class DashboardFragment extends FinFragment {

    @Override
    protected void render(LinearLayout root) {
        Context c = requireContext();
        List<FinTxn> all = store.txns();
        Period p = FinanceState.dash(all);
        walletCard(root, c);
        Ui.add(root, new Widgets.PeriodBar(c, p, true, all, this::refresh), 8);
        root.addView(Widgets.spacer(c, 16));
        if (all.isEmpty()) {
            emptyState(root);
            return;
        }

        Analytics.Totals t = Analytics.totals(all, p);
        Analytics.Totals prev = p.all ? null : Analytics.totals(all, p.shift(-1));

        String spentNote;
        int spentColor = R.color.fin_text_dim;
        if (prev == null) spentNote = "all time";
        else {
            spentNote = Widgets.changeNote(t.spent, prev.spent);
            spentColor = t.spent > prev.spent ? R.color.fin_red : R.color.fin_green_text;
        }
        Ui.twoUp(c, root,
                Ui.stat(c, "Spent", Fmt.whole(t.spent), spentNote, spentColor),
                Ui.stat(c, "Received", Fmt.whole(t.received), t.receivedCount + " credits", R.color.fin_green_text));
        boolean positive = t.net() >= 0;
        Ui.twoUp(c, root,
                Ui.stat(c, "Net flow", (positive ? "" : "-") + Fmt.money(t.net()),
                        positive ? "positive month" : "negative month",
                        positive ? R.color.fin_green_text : R.color.fin_red),
                Ui.stat(c, "Transactions", String.valueOf(t.count),
                        t.sentCount + " sent · " + t.receivedCount + " received", R.color.fin_text_dim));

        trendCard(root, c, all, p);
        if (!p.all) paceCard(root, c, all, p, t, prev);
        receivedFromCard(root, c, all, p);
        methodsCard(root, c, all, p);
        behaviourCard(root, c, all, p);
    }

    /** GPay-style wallet balance with quick actions for the original payment features. */
    private void walletCard(LinearLayout root, Context c) {
        LinearLayout card = Ui.card(c, root);
        card.setBackgroundResource(R.drawable.bg_balance_card);
        card.addView(Ui.text(c, "Wallet balance", 15, R.color.fin_text_dim, false));
        final TextView balance = Ui.text(c, "₹—", 38, R.color.white, true);
        Ui.add(card, balance, 4);

        Long backendId = com.arpit.myapplication.ServiceLocator.provideUserRepository().getBackendUserId();
        if (backendId != null) {
            com.arpit.myapplication.ServiceLocator.provideWalletRepository()
                    .getBalanceAsync(String.valueOf(backendId), (ok, bal) -> {
                        if (ok && isAdded()) balance.setText("₹" + Fmt.whole(bal).substring(1));
                    });
        }

        Object[][] actions = {
                {"Send", R.drawable.ic_send, com.arpit.myapplication.SendMoneyActivity.class},
                {"Add money", R.drawable.ic_add, com.arpit.myapplication.AddMoneyActivity.class},
                {"Balance", R.drawable.ic_wallet, com.arpit.myapplication.CheckBalanceActivity.class},
                {"History", R.drawable.ic_history, com.arpit.myapplication.TransactionsActivity.class},
                {"Split", R.drawable.ic_split, com.arpit.myapplication.SplitActivity.class},
                {"Tracking", R.drawable.ic_nav_insights, com.arpit.myapplication.TrackingActivity.class},
        };
        for (int r = 0; r < 2; r++) {
            LinearLayout line = new LinearLayout(c);
            for (int k = 0; k < 3; k++) {
                final Object[] a = actions[r * 3 + k];
                LinearLayout item = new LinearLayout(c);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER_HORIZONTAL);
                android.widget.ImageView icon = new android.widget.ImageView(c);
                icon.setImageResource((Integer) a[1]);
                icon.setColorFilter(Ui.color(c, R.color.fin_blue));
                icon.setBackgroundResource(R.drawable.bg_icon_circle);
                int pad = Ui.dp(c, 16);
                icon.setPadding(pad, pad, pad, pad);
                item.addView(icon, new LinearLayout.LayoutParams(Ui.dp(c, 60), Ui.dp(c, 60)));
                TextView label = Ui.text(c, (String) a[0], 14, R.color.fin_text, false);
                item.addView(label, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 8, c));
                item.setOnClickListener(v -> startActivity(new android.content.Intent(c, (Class<?>) a[2])));
                line.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            }
            Ui.add(card, line, r == 0 ? 20 : 16);
        }
    }

    private void trendCard(LinearLayout root, Context c, List<FinTxn> all, Period p) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Monthly trend"));
        Ui.add(card, Ui.sub(c, "Spent vs received, last 12 months"), 4);
        Analytics.Trend tr = Analytics.monthlyTrend(all, p);
        ChartViews.BarChart chart = new ChartViews.BarChart(c);
        chart.setData(tr.labels, tr.spent, tr.received);
        Ui.add(card, chart, 14);
        LinearLayout legend = new LinearLayout(c);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        legend.addView(dot(c, R.color.fin_red));
        legend.addView(Ui.text(c, "  Spent", 16, R.color.fin_text_dim, false));
        legend.addView(Widgets.spacer(c, 1), new LinearLayout.LayoutParams(Ui.dp(c, 22), 1));
        legend.addView(dot(c, R.color.fin_green));
        legend.addView(Ui.text(c, "  Received", 16, R.color.fin_text_dim, false));
        Ui.add(card, legend, 10);
    }

    private TextView dot(Context c, int color) {
        TextView d = new TextView(c);
        d.setBackgroundColor(Ui.color(c, color));
        d.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, 14), Ui.dp(c, 14)));
        return d;
    }

    private void paceCard(LinearLayout root, Context c, List<FinTxn> all, Period p,
                          Analytics.Totals t, Analytics.Totals prev) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Spending pace"));
        boolean current = p.isCurrentMonth();
        Ui.add(card, Ui.sub(c, current ? "Month to date, projected to month-end" : "How " + p.label() + " went"), 4);
        int dim = p.daysInMonth();
        int days = current ? java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH) : dim;
        double daily = t.spent / Math.max(1, days);
        Ui.add(card, Ui.kv(c, "Daily average", Fmt.money(daily), null), 16);
        if (current) Ui.add(card, Ui.kv(c, "Projected month-end", Fmt.whole(daily * dim), "at this pace"), 12);
        else Ui.add(card, Ui.kv(c, "Month total", Fmt.money(t.spent), null), 12);
        if (prev != null) Ui.add(card, Ui.kv(c, "Last month", Fmt.money(prev.spent), null), 12);
        double budget = store.budget("overall", p.key());
        if (budget > 0) {
            Ui.add(card, Ui.barRow(c, "Budget", Fmt.compact(t.spent) + " / " + Fmt.compact(budget),
                    t.spent / budget, t.spent > budget ? R.color.fin_red : R.color.fin_blue), 16);
        }
    }

    private void receivedFromCard(LinearLayout root, Context c, List<FinTxn> all, Period p) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Received from"));
        Ui.add(card, Ui.sub(c, "Who paid you, " + (p.all ? "all time" : p.label().substring(0, 3))), 4);
        List<Analytics.Entry> list = Analytics.group(all, p, true, false);
        if (list.isEmpty()) {
            Ui.add(card, Ui.empty(c, "No credits in this period."), 10);
            return;
        }
        double max = list.get(0).total;
        for (int i = 0; i < Math.min(4, list.size()); i++) {
            Analytics.Entry e = list.get(i);
            Ui.add(card, Ui.barRow(c, Widgets.describe(e), Fmt.compact(e.total), e.total / max, R.color.fin_green), i == 0 ? 18 : 16);
        }
    }

    private void methodsCard(LinearLayout root, Context c, List<FinTxn> all, Period p) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Payment methods"));
        Ui.add(card, Ui.sub(c, "Where the money left from"), 4);
        List<Analytics.Entry> list = Analytics.group(all, p, false, true);
        if (list.isEmpty()) {
            Ui.add(card, Ui.empty(c, "No spending in this period."), 10);
            return;
        }
        double max = list.get(0).total;
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Analytics.Entry e = list.get(i);
            Ui.add(card, Ui.barRow(c, Widgets.describe(e), Fmt.compact(e.total), e.total / max, R.color.fin_blue), i == 0 ? 18 : 16);
        }
    }

    private void behaviourCard(LinearLayout root, Context c, List<FinTxn> all, Period p) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Behaviour"));
        Ui.add(card, Ui.sub(c, "Habits, " + (p.all ? "all time" : p.label().substring(0, 3))), 4);
        Analytics.Behaviour b = Analytics.behaviour(all, p);
        if (b.biggest == null) {
            Ui.add(card, Ui.empty(c, "No spending in this period."), 10);
            return;
        }
        Ui.add(card, Ui.kv(c, "Average payment", Fmt.money(b.average), null), 18);
        Ui.add(card, Ui.kv(c, "Biggest expense", Fmt.compact(b.biggest.amount) + " · " + b.biggest.counterparty,
                Fmt.shortDate(b.biggest.time)), 14);
        Ui.add(card, Ui.kv(c, "Weekday vs weekend", Fmt.compact(b.weekday) + " / " + Fmt.compact(b.weekend),
                "most spending happens on " + (b.weekday >= b.weekend ? "weekdays" : "weekends")), 14);
        Ui.add(card, Ui.kv(c, "Active spending days", b.activeDays + " days", null), 14);
        Ui.add(card, Ui.kv(c, "New merchants", String.valueOf(b.newMerchants),
                "paid for the first time " + (p.all ? "overall" : "this month")), 14);
    }
}
