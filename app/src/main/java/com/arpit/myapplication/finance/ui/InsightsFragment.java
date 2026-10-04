package com.arpit.myapplication.finance.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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

public class InsightsFragment extends FinFragment {

    @Override
    protected void render(LinearLayout root) {
        Context c = requireContext();
        List<FinTxn> all = store.txns();
        Period p = FinanceState.insights();
        Ui.add(root, new Widgets.PeriodBar(c, p, true, all, this::refresh), 8);
        root.addView(Widgets.spacer(c, 16));
        if (all.isEmpty()) {
            emptyState(root);
            return;
        }
        String scope = p.all ? "all time" : p.label();
        hoursCard(root, c, all, p, scope);
        heatCard(root, c, all, p, scope);
        weeklyCard(root, c, all, p, scope);
        sizesCard(root, c, all, p, scope);
    }

    private void hoursCard(LinearLayout root, Context c, List<FinTxn> all, Period p, String scope) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "When do you spend?"));
        double[] h = Analytics.hourSpend(all, p);
        int peak = 0;
        for (int i = 1; i < 24; i++) if (h[i] > h[peak]) peak = i;
        String sub = h[peak] > 0 ? "Peak hour: " + Fmt.hourRange(peak) + " (" + Fmt.compact(h[peak]) + "), " + scope
                : "No spending in this period";
        Ui.add(card, Ui.sub(c, sub), 4);
        ChartViews.HourBars bars = new ChartViews.HourBars(c);
        bars.setData(h);
        Ui.add(card, bars, 14);
    }

    private void heatCard(LinearLayout root, Context c, List<FinTxn> all, Period p, String scope) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Week heatmap"));
        Ui.add(card, Ui.sub(c, "Day × hour spending pattern, " + scope + " — tap any cell"), 4);
        final Analytics.Heat heat = Analytics.heat(all, p);
        int bd = 0, bh = 0;
        for (int d = 0; d < 7; d++) for (int h = 0; h < 24; h++) if (heat.amount[d][h] > heat.amount[bd][bh]) {
            bd = d;
            bh = h;
        }
        final TextView tip = tooltip(c);
        Ui.add(card, tip, 14);
        final String[] names = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        final ChartViews.Heatmap map = new ChartViews.Heatmap(c);
        map.setListener((d, h) -> setTip(tip, names[d] + " · " + Fmt.hourRange(h),
                Fmt.money(heat.amount[d][h]) + " · " + heat.count[d][h] + " payments"));
        map.setData(heat.amount, bd, bh);
        setTip(tip, names[bd] + " · " + Fmt.hourRange(bh),
                Fmt.money(heat.amount[bd][bh]) + " · " + heat.count[bd][bh] + " payments");
        Ui.add(card, map, 10);

        LinearLayout legend = new LinearLayout(c);
        legend.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        legend.addView(Ui.text(c, "less  ", 14, R.color.fin_text_dim, false));
        int[] cols = {R.color.heat_0, R.color.heat_1, R.color.heat_2, R.color.heat_3, R.color.heat_4};
        for (int col : cols) {
            TextView sq = new TextView(c);
            sq.setBackgroundColor(Ui.color(c, col));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, 22), Ui.dp(c, 18));
            lp.leftMargin = Ui.dp(c, 4);
            legend.addView(sq, lp);
        }
        legend.addView(Ui.text(c, "  more", 14, R.color.fin_text_dim, false));
        Ui.add(card, legend, 6);
    }

    private void weeklyCard(LinearLayout root, Context c, List<FinTxn> all, Period p, String scope) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Weekly trend"));
        Ui.add(card, Ui.sub(c, "Total spent per week, last 26 weeks (" + scope + ")"), 4);
        final Analytics.Weekly w = Analytics.weekly(all, p, 26);
        final TextView tip = tooltip(c);
        Ui.add(card, tip, 14);
        int sel = w.total.length - 1;
        setTip(tip, Fmt.shortDate(w.weekStart[sel]), Fmt.compact(w.total[sel]));
        ChartViews.LineChart chart = new ChartViews.LineChart(c);
        chart.setListener(i -> setTip(tip, Fmt.shortDate(w.weekStart[i]), Fmt.compact(w.total[i])));
        chart.setData(w.total, Fmt.shortDate(w.weekStart[0]), Fmt.shortDate(w.weekStart[w.weekStart.length - 1]), sel);
        Ui.add(card, chart, 10);
    }

    private void sizesCard(LinearLayout root, Context c, List<FinTxn> all, Period p, String scope) {
        LinearLayout card = Ui.card(c, root);
        card.addView(Ui.title(c, "Payment sizes"));
        Ui.add(card, Ui.sub(c, "How your payments are distributed, " + scope), 4);
        Analytics.Sizes s = Analytics.sizes(all, p);
        int max = 1;
        for (int n : s.count) max = Math.max(max, n);
        StringBuilder byValue = new StringBuilder("By value: ");
        for (int i = 0; i < 5; i++) {
            Ui.add(card, Ui.barRow(c, Analytics.SIZE_LABELS[i], s.count[i] + " txns", s.count[i] / (double) max, R.color.fin_purple), i == 0 ? 18 : 14);
            if (i > 0) byValue.append(" · ");
            byValue.append(Analytics.SIZE_LABELS[i]).append(": ").append(Fmt.compact(s.sum[i]));
        }
        Ui.add(card, Ui.sub(c, byValue.toString()), 16);
    }

    private TextView tooltip(Context c) {
        TextView t = new TextView(c);
        t.setBackgroundResource(R.drawable.bg_tooltip);
        int h = Ui.dp(c, 16), v = Ui.dp(c, 12);
        t.setPadding(h, v, h, v);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private void setTip(TextView t, String line1, String line2) {
        SpannableStringBuilder sb = new SpannableStringBuilder(line1 + "\n" + line2);
        sb.setSpan(new ForegroundColorSpan(Ui.color(requireContext(), R.color.fin_text_dim)), 0, line1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), line1.length() + 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        t.setTextColor(Ui.color(requireContext(), R.color.fin_text));
        t.setTextSize(17);
        t.setText(sb);
    }
}
