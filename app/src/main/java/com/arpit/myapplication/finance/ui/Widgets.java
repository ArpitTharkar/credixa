package com.arpit.myapplication.finance.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arpit.myapplication.R;
import com.arpit.myapplication.finance.Analytics;
import com.arpit.myapplication.finance.FinTxn;
import com.arpit.myapplication.finance.Fmt;
import com.arpit.myapplication.finance.Period;

import java.util.ArrayList;
import java.util.List;

/** Reusable controls: month selector and segmented switch. */
public final class Widgets {
    private Widgets() {}

    /** [ < ] [ Aug 2026 v ] [ > ] [ All ] */
    public static class PeriodBar extends LinearLayout {
        private final Period period;
        private final boolean allowAll;
        private final Runnable onChange;
        private final List<FinTxn> data;
        private TextView label, allBtn;
        private ImageButton prev, next;

        public PeriodBar(Context c, Period period, boolean allowAll, List<FinTxn> data, Runnable onChange) {
            super(c);
            this.period = period;
            this.allowAll = allowAll;
            this.data = data;
            this.onChange = onChange;
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            int h = Ui.dp(c, 54);

            prev = arrow(c, R.drawable.ic_chevron_left);
            addView(prev, new LayoutParams(Ui.dp(c, 54), h));

            label = Ui.text(c, "", 17, R.color.fin_text, true);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER);
            label.setBackgroundResource(R.drawable.bg_pill);
            label.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_dropdown, 0);
            label.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Ui.color(c, R.color.fin_text_dim)));
            label.setPadding(Ui.dp(c, 10), 0, Ui.dp(c, 8), 0);
            LayoutParams lp = new LayoutParams(0, h, 1);
            lp.leftMargin = lp.rightMargin = Ui.dp(c, 8);
            addView(label, lp);

            next = arrow(c, R.drawable.ic_chevron_right);
            addView(next, new LayoutParams(Ui.dp(c, 54), h));

            if (allowAll) {
                allBtn = Ui.text(c, "All", 18, R.color.fin_text, true);
                allBtn.setGravity(Gravity.CENTER);
                LayoutParams ap = new LayoutParams(Ui.dp(c, 66), h);
                ap.leftMargin = Ui.dp(c, 8);
                addView(allBtn, ap);
                allBtn.setOnClickListener(v -> {
                    period.all = !period.all;
                    if (!period.all && period.year == 0) {
                        Period n = Period.now();
                        period.year = n.year;
                        period.month = n.month;
                    }
                    changed();
                });
            }

            prev.setOnClickListener(v -> step(-1));
            next.setOnClickListener(v -> step(1));
            label.setOnClickListener(v -> pickMonth());
            refresh();
        }

        private ImageButton arrow(Context c, int icon) {
            ImageButton b = new ImageButton(c);
            b.setImageResource(icon);
            b.setBackgroundResource(R.drawable.bg_pill);
            b.setColorFilter(Ui.color(c, R.color.fin_text));
            return b;
        }

        private void step(int d) {
            if (period.all) period.all = false;
            Period p = period.shift(d);
            if (p.isAfterNow()) return;
            period.year = p.year;
            period.month = p.month;
            changed();
        }

        private void pickMonth() {
            Period n = Period.now();
            long first = System.currentTimeMillis();
            for (FinTxn t : data) first = Math.min(first, t.time);
            Period f = Period.ofTime(first);
            int months = Math.min(36, (n.year - f.year) * 12 + (n.month - f.month) + 1);
            final List<Period> list = new ArrayList<>();
            String[] labels = new String[months];
            for (int i = 0; i < months; i++) {
                Period p = n.shift(-i);
                list.add(p);
                labels[i] = p.label();
            }
            new AlertDialog.Builder(getContext()).setTitle("Select month").setItems(labels, (d, i) -> {
                period.all = false;
                period.year = list.get(i).year;
                period.month = list.get(i).month;
                changed();
            }).show();
        }

        private void changed() {
            refresh();
            if (onChange != null) onChange.run();
        }

        private void refresh() {
            label.setText(period.label());
            boolean atEnd = !period.all && period.shift(1).isAfterNow();
            next.setEnabled(!atEnd && !period.all);
            next.setAlpha(next.isEnabled() ? 1f : 0.35f);
            prev.setAlpha(1f);
            if (allBtn != null) {
                allBtn.setBackgroundResource(period.all ? R.drawable.bg_pill_selected : R.drawable.bg_pill);
            }
        }
    }

    /** Pill-shaped switch with N labels. */
    public static class Segmented extends LinearLayout {
        public interface OnSelect { void onSelect(int index); }

        private final List<TextView> items = new ArrayList<>();

        public Segmented(Context c, String[] labels, int selected, OnSelect cb) {
            super(c);
            setOrientation(HORIZONTAL);
            setBackgroundResource(R.drawable.bg_segment);
            int pad = Ui.dp(c, 5);
            setPadding(pad, pad, pad, pad);
            for (int i = 0; i < labels.length; i++) {
                TextView t = Ui.text(c, labels[i], labels.length > 3 ? 15 : 17, R.color.fin_text_dim, false);
                t.setSingleLine(true);
                t.setGravity(Gravity.CENTER);
                t.setMinHeight(Ui.dp(c, 46));
                final int idx = i;
                t.setOnClickListener(v -> {
                    select(idx);
                    cb.onSelect(idx);
                });
                addView(t, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                items.add(t);
            }
            select(selected);
        }

        private void select(int sel) {
            for (int i = 0; i < items.size(); i++) {
                TextView t = items.get(i);
                boolean on = i == sel;
                t.setBackgroundResource(on ? R.drawable.bg_segment_selected : 0);
                t.setTextColor(Ui.color(getContext(), on ? R.color.fin_text : R.color.fin_text_dim));
                t.setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            }
        }
    }

    /** Percent change footnote for the dashboard stat tile. */
    public static String changeNote(double now, double before) {
        if (before <= 0) return "no data last month";
        double pct = (now - before) / before * 100;
        return (pct >= 0 ? "▲ " : "▼ ") + Math.round(Math.abs(pct)) + "% vs last month";
    }

    public static String describe(Analytics.Entry e) {
        return e.name + " · " + e.count + "×";
    }

    public static String when(long t) { return Fmt.dateTime(t); }

    public static View spacer(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, Ui.dp(c, dp)));
        return v;
    }
}
