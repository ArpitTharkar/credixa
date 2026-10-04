package com.arpit.myapplication.finance.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arpit.myapplication.R;
import com.arpit.myapplication.finance.Analytics;
import com.arpit.myapplication.finance.Categorizer;
import com.arpit.myapplication.finance.FinTxn;
import com.arpit.myapplication.finance.FinanceState;
import com.arpit.myapplication.finance.FinanceStore;
import com.arpit.myapplication.finance.Fmt;
import com.arpit.myapplication.finance.Period;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class PlannerFragment extends FinFragment {
    private static int tab = 0;
    private static final String[] HOLDING_TYPES = {"Stocks", "Mutual Funds", "FD / Bonds", "Gold", "Crypto", "Other"};

    @Override
    protected void render(LinearLayout root) {
        Context c = requireContext();
        List<FinTxn> all = store.txns();
        Ui.add(root, new Widgets.Segmented(c, new String[]{"Budgets", "Salary", "Portfolio"}, tab, i -> {
            tab = i;
            refresh();
        }), 8);
        root.addView(Widgets.spacer(c, 14));
        if (tab == 0) budgets(root, c, all);
        else if (tab == 1) salary(root, c, all);
        else portfolio(root, c);
    }

    // =================== Budgets ===================

    private void budgets(LinearLayout root, Context c, List<FinTxn> all) {
        Period p = FinanceState.planner();
        Ui.add(root, new Widgets.PeriodBar(c, p, false, all, this::refresh), 0);
        root.addView(Widgets.spacer(c, 14));
        overallCard(root, c, all, p);
        categoryCard(root, c, all, p);
        recurringCard(root, c, all);
        goalsCard(root, c, all);
    }

    private void overallCard(LinearLayout root, Context c, List<FinTxn> all, Period p) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Monthly budget"));
        Ui.add(card, Ui.sub(c, p.label()), 4);
        double limit = store.budget("overall", p.key());
        double spent = Analytics.totals(all, p).spent;
        if (limit <= 0) {
            Ui.add(card, Ui.sub(c, "No overall budget set for this month yet."), 14);
            TextView b = Ui.button(c, "Set overall budget", R.drawable.bg_button_blue);
            b.setOnClickListener(v -> editBudget("overall", "Overall budget", p, all));
            Ui.add(card, b, 16);
            return;
        }
        boolean over = spent > limit;
        TextView big = Ui.text(c, Fmt.whole(spent) + " of " + Fmt.whole(limit), 26, over ? R.color.fin_red : R.color.fin_text, true);
        Ui.add(card, big, 14);
        Ui.add(card, Ui.barRow(c, "", Math.round(spent / limit * 100) + "% used", spent / limit,
                over ? R.color.fin_red : R.color.fin_blue), 4);
        if (over) Ui.add(card, Ui.kv(c, "Over by", Fmt.money(spent - limit), null), 14);
        else {
            Ui.add(card, Ui.kv(c, "Remaining", Fmt.money(limit - spent), null), 14);
            if (p.isCurrentMonth()) {
                int left = Math.max(1, p.daysInMonth() - Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + 1);
                Ui.add(card, Ui.kv(c, "Daily allowance", Fmt.money((limit - spent) / left), left + " days left"), 12);
            }
        }
        TextView edit = Ui.text(c, "Edit budget", 17, R.color.fin_blue, true);
        edit.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
        edit.setOnClickListener(v -> editBudget("overall", "Overall budget", p, all));
        Ui.add(card, edit, 12);
    }

    private void categoryCard(LinearLayout root, Context c, List<FinTxn> all, Period p) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Category budgets"));
        Ui.add(card, Ui.sub(c, "Tap any budget to edit — suggestions come from your history"), 4);
        Ui.add(card, Ui.sub(c, "Add a budget to any category below — it applies every month unless you override it for a specific month."), 12);
        Map<String, Double> spend = Analytics.categorySpend(all, p);
        ChipGroup chips = new ChipGroup(c);
        boolean first = true;
        for (final String cat : Categorizer.BUDGET_CATEGORIES) {
            final double limit = store.budget(cat, p.key());
            if (limit > 0) {
                Double s = spend.get(cat);
                double used = s == null ? 0 : s;
                View row = Ui.barRow(c, cat, Fmt.compact(used) + " / " + Fmt.compact(limit), used / limit,
                        used > limit ? R.color.fin_red : R.color.fin_blue);
                row.setOnClickListener(v -> editBudget(cat, cat + " budget", p, all));
                Ui.add(card, row, first ? 18 : 14);
                first = false;
            } else {
                Chip chip = new Chip(c);
                chip.setText(cat);
                chip.setTextColor(Ui.color(c, R.color.fin_text));
                chip.setTextSize(16);
                chip.setChipBackgroundColor(ColorStateList.valueOf(Ui.color(c, R.color.fin_surface_alt)));
                chip.setChipStrokeColor(ColorStateList.valueOf(Ui.color(c, R.color.fin_stroke)));
                chip.setChipStrokeWidth(Ui.dp(c, 1));
                chip.setChipIconResource(R.drawable.ic_add);
                chip.setChipIconTint(ColorStateList.valueOf(Ui.color(c, R.color.fin_blue)));
                chip.setChipIconVisible(true);
                chip.setChipMinHeight(Ui.dp(c, 46));
                chip.setOnClickListener(v -> editBudget(cat, cat + " budget", p, all));
                chips.addView(chip);
            }
        }
        Ui.add(card, chips, 16);
    }

    private void editBudget(String key, String title, Period p, List<FinTxn> all) {
        Context c = requireContext();
        double current = store.budget(key, p.key());
        double suggestion;
        if (key.equals("overall")) {
            java.util.Set<String> months = new java.util.HashSet<>();
            double sum = 0;
            for (FinTxn t : all) {
                months.add(Period.ofTime(t.time).key());
                if (t.isSpend()) sum += t.amount;
            }
            suggestion = months.isEmpty() ? 0 : sum / months.size();
        } else suggestion = Analytics.averageMonthlyCategorySpend(all, key);
        final EditText amount = Ui.input(c, "Monthly amount (₹)", true);
        if (current > 0) amount.setText(String.valueOf(Math.round(current)));
        final CheckBox only = new CheckBox(c);
        only.setText("Only for " + p.label());
        only.setChecked(store.hasMonthOverride(key, p.key()));
        TextView hint = Ui.sub(c, suggestion > 0 ? "Suggested: " + Fmt.whole(Math.ceil(suggestion / 100) * 100) + " (your monthly average)" : "No history yet to suggest from.");
        AlertDialog.Builder b = new AlertDialog.Builder(c).setTitle(title).setView(Ui.form(c, amount, hint, only))
                .setPositiveButton("Save", (d, w) -> {
                    double v = Ui.parse(amount);
                    if (Double.isNaN(v) || v <= 0) {
                        Toast.makeText(c, "Enter a valid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    store.setBudget(key, only.isChecked() ? p.key() : null, v);
                    refresh();
                }).setNegativeButton("Cancel", null);
        if (current > 0) b.setNeutralButton("Remove", (d, w) -> {
            store.setBudget(key, only.isChecked() ? p.key() : null, 0);
            if (!only.isChecked()) store.setBudget(key, p.key(), 0);
            refresh();
        });
        b.show();
    }

    // ---- recurring ----

    private void recurringCard(LinearLayout root, Context c, List<FinTxn> all) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Recurring payments"));
        List<Analytics.Recurring> rec = Analytics.recurring(all);
        if (rec.isEmpty()) {
            Ui.add(card, Ui.sub(c, "Payments that repeat roughly monthly (3+ times) show up here automatically."), 6);
            return;
        }
        double expected = 0, paid = 0;
        for (Analytics.Recurring r : rec) {
            expected += r.amount;
            paid += r.paidThisMonth;
        }
        Ui.add(card, Ui.sub(c, "~" + Fmt.compact(expected) + "/month expected · " + Fmt.money(paid) + " paid this month"), 4);
        int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        for (Analytics.Recurring r : rec) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(Ui.iconSquare(c, R.drawable.ic_repeat, R.color.fin_blue));
            LinearLayout mid = new LinearLayout(c);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setPadding(Ui.dp(c, 14), 0, Ui.dp(c, 8), 0);
            TextView name = Ui.text(c, r.name, 18, R.color.fin_text, true);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            mid.addView(name);
            mid.addView(Ui.text(c, "~day " + r.dayOfMonth + " · every ~" + r.everyDays + "d · " + r.times + "× so far", 14, R.color.fin_text_dim, false));
            row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            LinearLayout right = new LinearLayout(c);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);
            right.addView(Ui.text(c, Fmt.compact(r.amount), 18, R.color.fin_text, true));
            String status = r.paid ? "PAID" : (r.dayOfMonth < today ? "OVERDUE" : "UPCOMING");
            int color = r.paid ? R.color.fin_green_text : (r.dayOfMonth < today ? R.color.fin_red : R.color.fin_orange);
            right.addView(Ui.text(c, status, 14, color, true));
            row.addView(right);
            Ui.add(card, row, 16);
        }
    }

    // ---- goals ----

    private void goalsCard(LinearLayout root, Context c, List<FinTxn> all) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Savings goals"));
        Ui.add(card, Ui.sub(c, "Tracked against your net flow (received − spent)"), 4);
        List<FinanceStore.Goal> goals = store.goals();
        if (goals.isEmpty()) {
            Ui.add(card, Ui.sub(c, "Set a target — e.g. save ₹50,000 by December — and the app tracks it from your imported data."), 14);
        }
        for (final FinanceStore.Goal g : goals) {
            double saved = 0;
            for (FinTxn t : all) {
                if (t.isFailed() || t.time < g.startMs) continue;
                saved += t.received ? t.amount : -t.amount;
            }
            saved = Math.max(0, saved);
            double frac = g.target <= 0 ? 0 : saved / g.target;
            View row = Ui.barRow(c, g.name, Fmt.compact(saved) + " / " + Fmt.compact(g.target), frac,
                    frac >= 1 ? R.color.fin_green : R.color.fin_blue);
            row.setOnClickListener(v -> new AlertDialog.Builder(c).setTitle(g.name)
                    .setMessage("Target " + Fmt.money(g.target) + " by " + Fmt.shortDate(g.deadlineMs))
                    .setPositiveButton("Delete", (d, w) -> {
                        store.deleteGoal(g);
                        refresh();
                    }).setNegativeButton("Close", null).show());
            Ui.add(card, row, 16);
            long monthsLeft = Math.max(1, (g.deadlineMs - System.currentTimeMillis()) / (30L * Analytics.DAY));
            String hint = frac >= 1 ? "Goal reached" : "by " + Fmt.shortDate(g.deadlineMs) + " · need "
                    + Fmt.compact((g.target - saved) / monthsLeft) + "/month";
            Ui.add(card, Ui.text(c, hint, 14, R.color.fin_text_dim, false), 4);
        }
        TextView add = Ui.button(c, "Add goal", R.drawable.bg_button_blue);
        add.setOnClickListener(v -> addGoal());
        Ui.add(card, add, 16);
    }

    private void addGoal() {
        Context c = requireContext();
        final EditText name = Ui.input(c, "Goal name (e.g. New phone)", false);
        final EditText target = Ui.input(c, "Target amount (₹)", true);
        final long[] deadline = {System.currentTimeMillis() + 180L * Analytics.DAY};
        final TextView date = Ui.text(c, "Deadline: " + Fmt.shortDate(deadline[0]) + "  (tap to change)", 16, R.color.fin_blue, false);
        date.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(deadline[0]);
            new DatePickerDialog(c, (dp, y, m, d) -> {
                Calendar x = Calendar.getInstance();
                x.set(y, m, d, 12, 0, 0);
                deadline[0] = x.getTimeInMillis();
                date.setText("Deadline: " + Fmt.shortDate(deadline[0]) + "  (tap to change)");
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        new AlertDialog.Builder(c).setTitle("New savings goal").setView(Ui.form(c, name, target, date))
                .setPositiveButton("Save", (d, w) -> {
                    double t = Ui.parse(target);
                    String n = name.getText().toString().trim();
                    if (n.isEmpty() || Double.isNaN(t) || t <= 0) {
                        Toast.makeText(c, "Enter a name and a target", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FinanceStore.Goal g = new FinanceStore.Goal();
                    g.name = n;
                    g.target = t;
                    g.deadlineMs = deadline[0];
                    g.startMs = Period.now().start();
                    store.saveGoal(g);
                    refresh();
                }).setNegativeButton("Cancel", null).show();
    }

    // =================== Salary ===================

    private void salary(LinearLayout root, Context c, List<FinTxn> all) {
        List<FinTxn> credits = Analytics.salaryCredits(all);
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Salary"));
        if (credits.isEmpty()) {
            Ui.add(card, Ui.sub(c, "No salary credits found. Tag a received transaction as \"Salary\" in Transactions (or import data whose description contains \"salary\") and it shows up here."), 6);
            return;
        }
        double sum = 0;
        for (FinTxn t : credits) sum += t.amount;
        FinTxn last = credits.get(0);
        Ui.add(card, Ui.sub(c, credits.size() + " salary credits detected"), 4);
        Ui.add(card, Ui.kv(c, "Average salary", Fmt.money(sum / credits.size()), null), 16);
        Ui.add(card, Ui.kv(c, "Last credited", Fmt.money(last.amount), Fmt.shortDate(last.time)), 12);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(last.time);
        Ui.add(card, Ui.kv(c, "Usually arrives", "~day " + cal.get(Calendar.DAY_OF_MONTH), "of the month"), 12);

        LinearLayout hist = Ui.card(c, root);
        hist.addView(Ui.title(c, "Salary vs spending"));
        Ui.add(hist, Ui.sub(c, "How much of each salary you kept, last 6 months"), 4);
        Period now = Period.now();
        for (int i = 0; i < 6; i++) {
            Period p = now.shift(-i);
            double sal = 0;
            for (FinTxn t : credits) if (p.contains(t.time)) sal += t.amount;
            double spent = Analytics.totals(all, p).spent;
            String hint = sal > 0 ? "kept " + Math.round((sal - spent) / sal * 100) + "% · spent " + Fmt.compact(spent) : "spent " + Fmt.compact(spent);
            Ui.add(hist, Ui.kv(c, p.label(), sal > 0 ? Fmt.compact(sal) : "—", hint), i == 0 ? 16 : 12);
        }
    }

    // =================== Portfolio ===================

    private void portfolio(LinearLayout root, Context c) {
        List<FinanceStore.Holding> hs = store.holdings();
        double inv = 0, cur = 0;
        for (FinanceStore.Holding h : hs) {
            inv += h.invested;
            cur += h.current;
        }
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Portfolio"));
        Ui.add(card, Ui.sub(c, "Track what you have invested and what it is worth now"), 4);
        double pl = cur - inv;
        Ui.add(card, Ui.kv(c, "Invested", Fmt.money(inv), null), 16);
        Ui.add(card, Ui.kv(c, "Current value", Fmt.money(cur), null), 12);
        Ui.add(card, Ui.kv(c, "Profit / loss", (pl < 0 ? "-" : "+") + Fmt.money(pl),
                inv > 0 ? String.format(java.util.Locale.ENGLISH, "%.1f%%", pl / inv * 100) : null), 12);
        double viaPayments = 0;
        for (FinTxn t : store.txns()) if (t.isSpend() && "Investments".equals(t.category)) viaPayments += t.amount;
        if (viaPayments > 0) Ui.add(card, Ui.kv(c, "Paid to investment apps", Fmt.compact(viaPayments), "from your transactions"), 12);

        LinearLayout list = Ui.card(c, root);
        list.addView(Ui.title(c, "Holdings"));
        if (hs.isEmpty()) Ui.add(list, Ui.sub(c, "No holdings yet. Add stocks, mutual funds, FDs, gold…"), 6);
        for (final FinanceStore.Holding h : hs) {
            double hp = h.current - h.invested;
            View row = Ui.kv(c, h.name + "\n" + h.type, Fmt.compact(h.current),
                    (hp < 0 ? "-" : "+") + Fmt.compact(hp) + " · invested " + Fmt.compact(h.invested));
            row.setOnClickListener(v -> editHolding(h));
            Ui.add(list, row, 16);
        }
        TextView add = Ui.button(c, "Add holding", R.drawable.bg_button_blue);
        add.setOnClickListener(v -> editHolding(null));
        Ui.add(list, add, 16);
    }

    private void editHolding(final FinanceStore.Holding existing) {
        Context c = requireContext();
        final EditText name = Ui.input(c, "Name (e.g. Nifty 50 index fund)", false);
        final EditText invested = Ui.input(c, "Amount invested (₹)", true);
        final EditText current = Ui.input(c, "Current value (₹)", true);
        final Spinner type = new Spinner(c);
        type.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_spinner_dropdown_item, HOLDING_TYPES));
        if (existing != null) {
            name.setText(existing.name);
            invested.setText(String.valueOf(Math.round(existing.invested)));
            current.setText(String.valueOf(Math.round(existing.current)));
            for (int i = 0; i < HOLDING_TYPES.length; i++) if (HOLDING_TYPES[i].equals(existing.type)) type.setSelection(i);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(c).setTitle(existing == null ? "Add holding" : "Edit holding")
                .setView(Ui.form(c, name, type, invested, current))
                .setPositiveButton("Save", (d, w) -> {
                    double i = Ui.parse(invested), cv = Ui.parse(current);
                    String n = name.getText().toString().trim();
                    if (n.isEmpty() || Double.isNaN(i) || Double.isNaN(cv)) {
                        Toast.makeText(c, "Fill in name, invested and current value", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FinanceStore.Holding h = existing == null ? new FinanceStore.Holding() : existing;
                    h.name = n;
                    h.type = (String) type.getSelectedItem();
                    h.invested = i;
                    h.current = cv;
                    store.saveHolding(h);
                    refresh();
                }).setNegativeButton("Cancel", null);
        if (existing != null) b.setNeutralButton("Delete", (d, w) -> {
            store.deleteHolding(existing);
            refresh();
        });
        b.show();
    }
}
