package com.arpit.myapplication.finance.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.arpit.myapplication.R;

/** Tiny view factory so screens can be assembled in code in the dark finance style. */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int color(Context c, @ColorRes int res) { return ContextCompat.getColor(c, res); }

    public static LinearLayout.LayoutParams lp(int w, int h, int top, Context c) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(c, top);
        return p;
    }

    public static LinearLayout card(Context c, LinearLayout parent) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int p = dp(c, 18);
        card.setPadding(p, p, p, p);
        parent.addView(card, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, c));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).bottomMargin = dp(c, 14);
        return card;
    }

    public static TextView text(Context c, CharSequence s, float sp, @ColorRes int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color(c, color));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView title(Context c, String s) { return text(c, s, 21, R.color.fin_text, true); }

    public static TextView sub(Context c, String s) {
        TextView t = text(c, s, 15, R.color.fin_text_dim, false);
        t.setLineSpacing(0, 1.1f);
        return t;
    }

    public static void add(LinearLayout parent, View v, int topDp) {
        parent.addView(v, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, topDp, parent.getContext()));
    }

    /** Label on the left, value on the right, thin proportional bar underneath. */
    public static View barRow(Context c, String left, String right, double fraction, @ColorRes int barColor) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout top = new LinearLayout(c);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = text(c, left, 17, R.color.fin_text, false);
        l.setMaxLines(1);
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        top.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView r = text(c, right, 17, R.color.fin_text, true);
        r.setPadding(dp(c, 8), 0, 0, 0);
        top.addView(r);
        row.addView(top);

        LinearLayout track = new LinearLayout(c);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackgroundResource(R.drawable.bg_pill);
        float f = (float) Math.max(0, Math.min(1, fraction));
        View fill = new View(c);
        fill.setBackgroundColor(color(c, barColor));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(c, 6), f));
        track.addView(new View(c), new LinearLayout.LayoutParams(0, dp(c, 6), 1 - f));
        LinearLayout.LayoutParams tp = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 6), 6, c);
        row.addView(track, tp);
        return row;
    }

    /** Key on the left, value on the right, optional dim hint below the value. */
    public static View kv(Context c, String key, String value, String hint) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        TextView k = text(c, key, 17, R.color.fin_text_dim, false);
        row.addView(k, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        TextView v = text(c, value, 17, R.color.fin_text, true);
        v.setGravity(Gravity.END);
        right.addView(v);
        if (hint != null) {
            TextView h = text(c, hint, 14, R.color.fin_text_dim, false);
            h.setGravity(Gravity.END);
            right.addView(h);
        }
        row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));
        return row;
    }

    /** Rounded stat tile: label, big value and a coloured footnote. */
    public static View stat(Context c, String label, String value, String note, @ColorRes int noteColor) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_card);
        int p = dp(c, 18);
        box.setPadding(p, p, p, p);
        box.addView(text(c, label, 17, R.color.fin_text_dim, false));
        TextView v = text(c, value, 30, R.color.fin_text, true);
        v.setSingleLine(true);
        v.setAutoSizeTextTypeUniformWithConfiguration(16, 30, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        box.addView(v, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 10, c));
        TextView n = text(c, note, 14, noteColor, false);
        box.addView(n, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 6, c));
        return box;
    }

    public static LinearLayout twoUp(Context c, LinearLayout parent, View a, View b) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        l.rightMargin = dp(c, 7);
        LinearLayout.LayoutParams r = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        r.leftMargin = dp(c, 7);
        row.addView(a, l);
        row.addView(b, r);
        parent.addView(row, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, c));
        ((LinearLayout.LayoutParams) row.getLayoutParams()).bottomMargin = dp(c, 14);
        return row;
    }

    public static TextView button(Context c, String label, @DrawableRes int bg) {
        TextView b = text(c, label, 18, R.color.white, true);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(bg);
        b.setMinHeight(dp(c, 58));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    public static View iconSquare(Context c, @DrawableRes int icon, @ColorRes int tint) {
        LinearLayout box = new LinearLayout(c);
        box.setBackgroundResource(R.drawable.bg_icon_square);
        box.setGravity(Gravity.CENTER);
        ImageView iv = new ImageView(c);
        iv.setImageResource(icon);
        iv.setColorFilter(color(c, tint));
        box.addView(iv, new LinearLayout.LayoutParams(dp(c, 26), dp(c, 26)));
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 56), dp(c, 56)));
        return box;
    }

    public static TextView empty(Context c, String message) {
        TextView t = sub(c, message);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(c, 8), 0, dp(c, 8));
        return t;
    }

    // ---- dialogs ----

    public interface TextCallback { void onText(String value); }

    public static EditText input(Context c, String hint, boolean number) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        if (number) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    public static LinearLayout form(Context c, View... views) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 22);
        l.setPadding(p, dp(c, 10), p, 0);
        for (View v : views) l.addView(v, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 8, c));
        return l;
    }

    public static double parse(EditText e) {
        try {
            return Double.parseDouble(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    public static void choose(Context c, String title, String[] items, java.util.function.IntConsumer onPick) {
        new AlertDialog.Builder(c).setTitle(title).setItems(items, (d, i) -> onPick.accept(i)).show();
    }
}
