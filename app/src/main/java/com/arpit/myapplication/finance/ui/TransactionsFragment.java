package com.arpit.myapplication.finance.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arpit.myapplication.R;
import com.arpit.myapplication.finance.Categorizer;
import com.arpit.myapplication.finance.FinTxn;
import com.arpit.myapplication.finance.FinanceState;
import com.arpit.myapplication.finance.FinanceStore;
import com.arpit.myapplication.finance.Fmt;
import com.arpit.myapplication.finance.Period;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TransactionsFragment extends FinFragment {
    private static final int ALL = 0, SENT = 1, RECEIVED = 2, FIX = 3, FAILED = 4;
    private static int filter = ALL;

    private RecyclerView list;
    private TextView empty;
    private EditText search;
    private ImageButton selectBtn;
    private Adapter adapter;
    private boolean selectMode;
    private final Set<String> selected = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        Context c = requireContext();
        store = FinanceStore.get(c);
        View v = inflater.inflate(R.layout.fragment_transactions, container, false);
        list = v.findViewById(R.id.txnList);
        empty = v.findViewById(R.id.txnEmpty);
        search = v.findViewById(R.id.txnSearch);
        selectBtn = v.findViewById(R.id.txnSelect);
        list.setLayoutManager(new LinearLayoutManager(c));
        adapter = new Adapter();
        list.setAdapter(adapter);

        ((FrameLayout) v.findViewById(R.id.txnFilterHolder)).addView(new Widgets.Segmented(c,
                new String[]{"All", "Sent", "Received", "Fix", "Failed"}, filter, i -> {
            filter = i;
            rebuild();
        }));
        Period p = FinanceState.txns(store.txns());
        ((FrameLayout) v.findViewById(R.id.txnPeriodHolder)).addView(
                new Widgets.PeriodBar(c, p, true, store.txns(), this::rebuild));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { rebuild(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        selectBtn.setOnClickListener(x -> onSelectClicked());
        v.findViewById(R.id.txnFab).setOnClickListener(x -> addDialog());
        return v;
    }

    @Override
    protected void render(LinearLayout root) { /* uses its own layout */ }

    @Override
    public void refresh() {
        if (list == null || !isAdded()) return;
        store = FinanceStore.get(requireContext());
        rebuild();
    }

    private void rebuild() {
        Period p = FinanceState.txns(store.txns());
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Object> rows = new ArrayList<>();
        String lastDay = "";
        for (FinTxn t : store.txns()) {
            if (!p.contains(t.time)) continue;
            switch (filter) {
                case SENT: if (t.received) continue; break;
                case RECEIVED: if (!t.received) continue; break;
                case FIX: if (t.isFailed() || !Categorizer.OTHER.equals(t.category)) continue; break;
                case FAILED: if (!t.isFailed()) continue; break;
                default: break;
            }
            if (!q.isEmpty()) {
                String hay = (t.counterparty + " " + t.note + " " + t.category + " " + Fmt.money(t.amount)).toLowerCase(Locale.ROOT);
                if (!hay.contains(q) && !String.valueOf(Math.round(t.amount)).contains(q)) continue;
            }
            String day = Fmt.dayHeader(t.time);
            if (!day.equals(lastDay)) {
                rows.add(day);
                lastDay = day;
            }
            rows.add(t);
        }
        adapter.set(rows);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(store.count() == 0 ? "No transactions yet.\nImport data or add one with +." : "No transactions match.");
    }

    // ---- selection ----

    private void onSelectClicked() {
        Context c = requireContext();
        if (!selectMode) {
            selectMode = true;
            selected.clear();
            selectBtn.setBackgroundResource(R.drawable.bg_pill_selected);
            selectBtn.setColorFilter(Ui.color(c, R.color.white));
            Toast.makeText(c, "Tap transactions to select, then tap ✓ again", Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
            return;
        }
        if (selected.isEmpty()) {
            endSelect();
            return;
        }
        final Set<String> ids = new HashSet<>(selected);
        new AlertDialog.Builder(c).setTitle(ids.size() + " selected")
                .setItems(new String[]{"Set category", "Delete"}, (d, i) -> {
                    if (i == 0) pickCategory(ids);
                    else confirmDelete(ids);
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void endSelect() {
        selectMode = false;
        selected.clear();
        selectBtn.setBackgroundResource(R.drawable.bg_pill);
        selectBtn.setColorFilter(Ui.color(requireContext(), R.color.fin_blue));
        adapter.notifyDataSetChanged();
    }

    private void pickCategory(Set<String> ids) {
        Ui.choose(requireContext(), "Category", Categorizer.ALL_CATEGORIES, i -> {
            store.setCategory(ids, Categorizer.ALL_CATEGORIES[i]);
            endSelect();
            rebuild();
        });
    }

    private void confirmDelete(Set<String> ids) {
        new AlertDialog.Builder(requireContext()).setTitle("Delete " + ids.size() + " transaction(s)?")
                .setPositiveButton("Delete", (d, w) -> {
                    store.remove(ids);
                    endSelect();
                    rebuild();
                }).setNegativeButton("Cancel", null).show();
    }

    private void rowClicked(FinTxn t) {
        if (selectMode) {
            if (!selected.add(t.id)) selected.remove(t.id);
            adapter.notifyDataSetChanged();
            return;
        }
        final Set<String> ids = new HashSet<>();
        ids.add(t.id);
        String info = Fmt.signed(t.amount, t.received) + "\n" + Fmt.dateTime(t.time)
                + (t.method.isEmpty() ? "" : "\n" + t.method) + (t.note.isEmpty() ? "" : "\n" + t.note);
        new AlertDialog.Builder(requireContext()).setTitle(t.counterparty).setMessage(info)
                .setPositiveButton("Change category", (d, w) -> pickCategory(ids))
                .setNeutralButton("Delete", (d, w) -> confirmDelete(ids))
                .setNegativeButton("Close", null).show();
    }

    // ---- add manually ----

    private void addDialog() {
        Context c = requireContext();
        final EditText name = Ui.input(c, "Who / what for", false);
        final EditText amount = Ui.input(c, "Amount (₹)", true);
        final RadioGroup dir = new RadioGroup(c);
        dir.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton sent = new RadioButton(c);
        sent.setText("Sent");
        sent.setId(View.generateViewId());
        RadioButton recv = new RadioButton(c);
        recv.setText("Received");
        recv.setId(View.generateViewId());
        dir.addView(sent);
        dir.addView(recv);
        dir.check(sent.getId());
        final Spinner cat = new Spinner(c);
        cat.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_spinner_dropdown_item, Categorizer.ALL_CATEGORIES));
        cat.setSelection(Categorizer.ALL_CATEGORIES.length - 1);
        new AlertDialog.Builder(c).setTitle("Add transaction")
                .setView(Ui.form(c, name, amount, dir, cat))
                .setPositiveButton("Add", (d, w) -> {
                    double a = Ui.parse(amount);
                    String who = name.getText().toString().trim();
                    if (Double.isNaN(a) || a <= 0 || who.isEmpty()) {
                        Toast.makeText(c, "Enter a name and a valid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FinTxn t = new FinTxn();
                    t.time = System.currentTimeMillis();
                    t.amount = a;
                    t.received = dir.getCheckedRadioButtonId() == recv.getId();
                    t.counterparty = who;
                    t.category = (String) cat.getSelectedItem();
                    if (Categorizer.OTHER.equals(t.category)) t.category = Categorizer.categorize(who, "", t.received);
                    store.addManual(t);
                    FinanceState.reset();
                    rebuild();
                }).setNegativeButton("Cancel", null).show();
    }

    // ---- list ----

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Object> rows = new ArrayList<>();

        void set(List<Object> r) {
            rows = r;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) { return rows.get(position) instanceof String ? 0 : 1; }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (type == 0) return new RecyclerView.ViewHolder(inf.inflate(R.layout.item_fin_header, parent, false)) {};
            return new RecyclerView.ViewHolder(inf.inflate(R.layout.item_fin_txn, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
            Object o = rows.get(position);
            View v = h.itemView;
            if (o instanceof String) {
                ((TextView) v).setText((String) o);
                return;
            }
            final FinTxn t = (FinTxn) o;
            Context c = v.getContext();
            TextView title = v.findViewById(R.id.txnTitle), sub = v.findViewById(R.id.txnSub), amt = v.findViewById(R.id.txnAmount);
            ImageView icon = v.findViewById(R.id.txnIcon);
            View bg = v.findViewById(R.id.txnIconBg);
            title.setText(t.counterparty);
            sub.setText((t.isFailed() ? "Failed" : t.category) + " · " + Fmt.shortDateTime(t.time));
            amt.setText(Fmt.signed(t.amount, t.received));
            amt.setTextColor(Ui.color(c, t.received ? R.color.fin_green_text : R.color.fin_text));
            if (t.isFailed()) {
                amt.setTextColor(Ui.color(c, R.color.fin_text_dim));
                amt.setPaintFlags(amt.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                amt.setPaintFlags(amt.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
            icon.setImageResource(t.received ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
            icon.setColorFilter(Ui.color(c, t.received ? R.color.fin_green_text : R.color.fin_red));
            bg.setBackgroundResource(t.received ? R.drawable.bg_circle_green : R.drawable.bg_circle_red);
            v.setBackgroundColor(selected.contains(t.id) ? Ui.color(c, R.color.fin_surface_alt) : 0);
            v.setOnClickListener(x -> rowClicked(t));
        }

        @Override
        public int getItemCount() { return rows.size(); }
    }
}
