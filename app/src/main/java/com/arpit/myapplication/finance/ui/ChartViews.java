package com.arpit.myapplication.finance.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.arpit.myapplication.R;

/** Small hand-drawn chart views (no third-party chart library). */
public final class ChartViews {
    private ChartViews() {}

    abstract static class Base extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        final float density;
        final int dim, stroke, blue, red, green;

        Base(Context c) {
            super(c);
            density = c.getResources().getDisplayMetrics().density;
            dim = ContextCompat.getColor(c, R.color.fin_text_dim);
            stroke = ContextCompat.getColor(c, R.color.fin_stroke);
            blue = ContextCompat.getColor(c, R.color.fin_blue);
            red = ContextCompat.getColor(c, R.color.fin_red);
            green = ContextCompat.getColor(c, R.color.fin_green);
            text.setColor(dim);
            text.setTextSize(13 * density);
            text.setTextAlign(Paint.Align.CENTER);
        }

        float dp(float v) { return v * density; }

        void gridLine(Canvas canvas, float left, float right, float y) {
            paint.setColor(stroke);
            paint.setStrokeWidth(dp(1));
            canvas.drawLine(left, y, right, y, paint);
        }
    }

    /** Grouped spent/received bars per month. */
    public static class BarChart extends Base {
        private String[] labels = new String[0];
        private double[] a = new double[0], b = new double[0];

        public BarChart(Context c) { super(c); }

        public void setData(String[] labels, double[] spent, double[] received) {
            this.labels = labels;
            this.a = spent;
            this.b = received;
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(230));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int n = labels.length;
            if (n == 0) return;
            float left = dp(4), right = getWidth() - dp(4), top = dp(8), bottom = getHeight() - dp(28);
            double max = 1;
            for (int i = 0; i < n; i++) max = Math.max(max, Math.max(a[i], b[i]));
            gridLine(canvas, left, right, top);
            gridLine(canvas, left, right, top + (bottom - top) / 2);
            gridLine(canvas, left, right, bottom);
            float slot = (right - left) / n;
            float barW = Math.min(dp(36), slot * 0.36f);
            float gap = dp(4);
            for (int i = 0; i < n; i++) {
                float cx = left + slot * i + slot / 2;
                drawBar(canvas, cx - gap / 2 - barW, cx - gap / 2, bottom, top, a[i] / max, red);
                drawBar(canvas, cx + gap / 2, cx + gap / 2 + barW, bottom, top, b[i] / max, green);
                canvas.drawText(labels[i], cx, getHeight() - dp(6), text);
            }
        }

        private void drawBar(Canvas canvas, float l, float r, float bottom, float top, double frac, int color) {
            float h = (float) (frac * (bottom - top));
            if (h < dp(1) && frac > 0) h = dp(1);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(l, bottom - h, r, bottom + dp(3)), dp(5), dp(5), paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.fin_surface));
            canvas.drawRect(l - 1, bottom, r + 1, bottom + dp(4), paint); // flatten the bottom corners
            gridLine(canvas, l - 1, r + 1, bottom);
        }
    }

    /** 24 vertical bars, one per hour of day. */
    public static class HourBars extends Base {
        private double[] v = new double[24];

        public HourBars(Context c) { super(c); }

        public void setData(double[] values) {
            v = values;
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(200));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float left = dp(2), right = getWidth() - dp(2), top = dp(8), bottom = getHeight() - dp(26);
            double max = 1;
            for (double d : v) max = Math.max(max, d);
            gridLine(canvas, left, right, top);
            gridLine(canvas, left, right, top + (bottom - top) / 2);
            gridLine(canvas, left, right, bottom);
            float slot = (right - left) / 24f;
            float w = slot * 0.7f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(blue);
            for (int i = 0; i < 24; i++) {
                float h = (float) (v[i] / max * (bottom - top));
                if (v[i] > 0 && h < dp(3)) h = dp(3);
                float cx = left + slot * i + slot / 2;
                if (h > 0) canvas.drawRoundRect(new RectF(cx - w / 2, bottom - h, cx + w / 2, bottom), dp(3), dp(3), paint);
            }
            String[] labels = {"12a", "6a", "12p", "6p"};
            for (int i = 0; i < 4; i++) {
                float cx = left + slot * (i * 6) + slot / 2;
                text.setTextAlign(i == 0 ? Paint.Align.LEFT : Paint.Align.CENTER);
                canvas.drawText(labels[i], i == 0 ? left : cx, getHeight() - dp(6), text);
            }
            text.setTextAlign(Paint.Align.CENTER);
        }
    }

    /** Day-of-week by hour grid. Tap a cell to select it. */
    public static class Heatmap extends Base {
        public interface OnCell { void onCell(int day, int hour); }

        private double[][] data = new double[7][24];
        private int selDay = -1, selHour = -1;
        private OnCell listener;
        private final int[] scale = new int[5];
        private final float labelW;
        private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        public Heatmap(Context c) {
            super(c);
            scale[0] = ContextCompat.getColor(c, R.color.heat_0);
            scale[1] = ContextCompat.getColor(c, R.color.heat_1);
            scale[2] = ContextCompat.getColor(c, R.color.heat_2);
            scale[3] = ContextCompat.getColor(c, R.color.heat_3);
            scale[4] = ContextCompat.getColor(c, R.color.heat_4);
            labelW = dp(38);
        }

        public void setListener(OnCell l) { listener = l; }

        public void setData(double[][] d, int selectDay, int selectHour) {
            data = d;
            selDay = selectDay;
            selHour = selectHour;
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(7 * 22 + 30));
        }

        private float cellW() { return (getWidth() - labelW) / 24f; }

        private float cellH() { return dp(22); }

        @Override
        protected void onDraw(Canvas canvas) {
            double max = 0;
            for (double[] row : data) for (double v : row) max = Math.max(max, v);
            float cw = cellW(), ch = cellH(), pad = dp(1.5f);
            text.setTextAlign(Paint.Align.LEFT);
            for (int d = 0; d < 7; d++) {
                canvas.drawText(DAYS[d], 0, d * ch + ch * 0.72f, text);
                for (int h = 0; h < 24; h++) {
                    double v = data[d][h];
                    int level = 0;
                    if (v > 0 && max > 0) {
                        double f = v / max;
                        level = f < 0.15 ? 1 : f < 0.4 ? 2 : f < 0.7 ? 3 : 4;
                    }
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(scale[level]);
                    RectF r = new RectF(labelW + h * cw + pad, d * ch + pad, labelW + (h + 1) * cw - pad, (d + 1) * ch - pad);
                    canvas.drawRoundRect(r, dp(3), dp(3), paint);
                    if (d == selDay && h == selHour) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(0xFFFFFFFF);
                        paint.setStrokeWidth(dp(2));
                        canvas.drawRoundRect(r, dp(3), dp(3), paint);
                    }
                }
            }
            text.setTextAlign(Paint.Align.CENTER);
            String[] labels = {"12am", "6am", "12pm", "6pm"};
            for (int i = 0; i < 4; i++) {
                canvas.drawText(labels[i], labelW + (i * 6 + 1.5f) * cw, 7 * ch + dp(20), text);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                int h = (int) ((e.getX() - labelW) / cellW());
                int d = (int) (e.getY() / cellH());
                if (h >= 0 && h < 24 && d >= 0 && d < 7) {
                    selDay = d;
                    selHour = h;
                    invalidate();
                    if (listener != null) listener.onCell(d, h);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    /** Filled line chart with a draggable selection marker. */
    public static class LineChart extends Base {
        public interface OnPoint { void onPoint(int index); }

        private double[] v = new double[0];
        private String left = "", right = "";
        private int sel = -1;
        private OnPoint listener;

        public LineChart(Context c) { super(c); }

        public void setListener(OnPoint l) { listener = l; }

        public void setData(double[] values, String leftLabel, String rightLabel, int selected) {
            v = values;
            left = leftLabel;
            right = rightLabel;
            sel = selected;
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(190));
        }

        private float x(int i, float l, float r) { return v.length <= 1 ? l : l + (r - l) * i / (v.length - 1); }

        @Override
        protected void onDraw(Canvas canvas) {
            if (v.length == 0) return;
            float l = dp(2), r = getWidth() - dp(2), top = dp(10), bottom = getHeight() - dp(28);
            double max = 1;
            for (double d : v) max = Math.max(max, d);
            gridLine(canvas, l, r, top);
            gridLine(canvas, l, r, top + (bottom - top) / 2);
            gridLine(canvas, l, r, bottom);
            Path line = new Path(), area = new Path();
            for (int i = 0; i < v.length; i++) {
                float px = x(i, l, r), py = (float) (bottom - v[i] / max * (bottom - top));
                if (i == 0) {
                    line.moveTo(px, py);
                    area.moveTo(px, bottom);
                    area.lineTo(px, py);
                } else {
                    line.lineTo(px, py);
                    area.lineTo(px, py);
                }
            }
            area.lineTo(x(v.length - 1, l, r), bottom);
            area.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor((blue & 0x00FFFFFF) | 0x33000000);
            canvas.drawPath(area, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.5f));
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(blue);
            canvas.drawPath(line, paint);
            if (sel >= 0 && sel < v.length) {
                float px = x(sel, l, r), py = (float) (bottom - v[sel] / max * (bottom - top));
                paint.setStrokeWidth(dp(1.5f));
                paint.setColor(dim);
                paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(3), dp(4)}, 0));
                canvas.drawLine(px, top, px, bottom, paint);
                paint.setPathEffect(null);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(blue);
                canvas.drawCircle(px, py, dp(6), paint);
                paint.setColor(ContextCompat.getColor(getContext(), R.color.fin_surface));
                canvas.drawCircle(px, py, dp(2.5f), paint);
            }
            text.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(left, l, getHeight() - dp(6), text);
            text.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(right, r, getHeight() - dp(6), text);
            text.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (v.length == 0) return false;
            if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
                float l = dp(2), r = getWidth() - dp(2);
                int idx = Math.round((e.getX() - l) / (r - l) * (v.length - 1));
                idx = Math.max(0, Math.min(v.length - 1, idx));
                if (idx != sel) {
                    sel = idx;
                    invalidate();
                    if (listener != null) listener.onPoint(idx);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            return super.onTouchEvent(e);
        }
    }
}
