package com.arpit.myapplication.finance.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.arpit.myapplication.R;
import com.arpit.myapplication.finance.FinTxn;
import com.arpit.myapplication.finance.FinanceState;
import com.arpit.myapplication.finance.FinanceStore;
import com.arpit.myapplication.finance.Fmt;
import com.arpit.myapplication.finance.ImportParsers;
import com.arpit.myapplication.finance.SampleData;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportFragment extends FinFragment {
    private static final int CSV = 0, JSON = 1, TAKEOUT = 2, PDF = 3;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String[]> picker;
    private int pendingKind;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) importFile(uri, pendingKind);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    @Override
    protected void render(LinearLayout root) {
        Context c = requireContext();

        LinearLayout add = Ui.card(c, root);
        add.addView(Ui.title(c, "Add data"));
        Ui.add(add, Ui.sub(c, store.count() + " transactions on this device — nothing ever leaves it"), 4);
        option(add, c, R.drawable.ic_file, "GPay CSV export", "timestamp, action, amount, counterparty…", CSV);
        option(add, c, R.drawable.ic_code, "JSON export", "Same fields as the CSV, as a JSON array", JSON);
        option(add, c, 0, "Google Takeout activity", "Takeout → My Activity → Google Pay → My Activity.html", TAKEOUT);
        option(add, c, R.drawable.ic_file, "PDF statement", "Parsed on-device, no upload", PDF);

        View sample = row(c, R.drawable.ic_sample, null, "Load sample data", "8 months of demo transactions to explore the app");
        sample.setOnClickListener(v -> loadSample());
        Ui.add(add, sample, 14);

        LinearLayout hist = Ui.card(c, root);
        hist.addView(Ui.title(c, "Import history"));
        Ui.add(hist, Ui.sub(c, "Tap the undo icon to roll back a bad import"), 4);
        List<FinanceStore.ImportRec> imports = store.imports();
        if (imports.isEmpty()) Ui.add(hist, Ui.empty(c, "Nothing imported yet."), 10);
        for (final FinanceStore.ImportRec r : imports) {
            LinearLayout line = new LinearLayout(c);
            line.setGravity(Gravity.CENTER_VERTICAL);
            ImageView file = new ImageView(c);
            file.setImageResource(R.drawable.ic_file);
            file.setColorFilter(Ui.color(c, R.color.fin_text_dim));
            line.addView(file, new LinearLayout.LayoutParams(Ui.dp(c, 28), Ui.dp(c, 28)));
            LinearLayout mid = new LinearLayout(c);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setPadding(Ui.dp(c, 14), 0, Ui.dp(c, 8), 0);
            TextView name = Ui.text(c, r.name, 17, R.color.fin_text, false);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            mid.addView(name);
            mid.addView(Ui.text(c, Fmt.dateTime(r.time), 14, R.color.fin_text_dim, false));
            line.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            line.addView(Ui.text(c, "+" + r.count, 17, R.color.fin_text, true));
            ImageView undo = new ImageView(c);
            undo.setImageResource(R.drawable.ic_undo);
            undo.setColorFilter(Ui.color(c, R.color.fin_orange));
            undo.setPadding(Ui.dp(c, 14), Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8));
            undo.setOnClickListener(v -> new AlertDialog.Builder(c).setTitle("Undo this import?")
                    .setMessage("Removes the " + r.count + " transactions it added.")
                    .setPositiveButton("Undo", (d, w) -> {
                        store.removeImport(r.id);
                        FinanceState.reset();
                        refresh();
                    }).setNegativeButton("Cancel", null).show());
            line.addView(undo, new LinearLayout.LayoutParams(Ui.dp(c, 50), Ui.dp(c, 44)));
            Ui.add(hist, line, 16);
        }

        LinearLayout danger = Ui.card(c, root);
        danger.addView(Ui.title(c, "Danger zone"));
        TextView del = Ui.button(c, "Delete all transactions", R.drawable.bg_button_red);
        del.setOnClickListener(v -> new AlertDialog.Builder(c).setTitle("Delete all transactions?")
                .setMessage("This removes everything stored on this device for this account.")
                .setPositiveButton("Delete", (d, w) -> {
                    store.deleteAll();
                    FinanceState.reset();
                    refresh();
                }).setNegativeButton("Cancel", null).show());
        Ui.add(danger, del, 14);
    }

    private void option(LinearLayout parent, Context c, int icon, String title, String sub, final int kind) {
        View r = row(c, icon, icon == 0 ? "G" : null, title, sub);
        r.setOnClickListener(v -> {
            pendingKind = kind;
            picker.launch(kind == PDF ? new String[]{"application/pdf"} : new String[]{"*/*"});
        });
        Ui.add(parent, r, 14);
    }

    private View row(Context c, int icon, String letter, String title, String sub) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        int p = Ui.dp(c, 14);
        row.setPadding(p, p, p, p);
        if (letter != null) {
            LinearLayout box = (LinearLayout) Ui.iconSquare(c, R.drawable.ic_add, R.color.fin_blue);
            box.removeAllViews();
            TextView g = Ui.text(c, letter, 26, R.color.fin_blue, true);
            g.setGravity(Gravity.CENTER);
            box.addView(g);
            row.addView(box);
        } else {
            row.addView(Ui.iconSquare(c, icon, R.color.fin_blue));
        }
        LinearLayout mid = new LinearLayout(c);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(Ui.dp(c, 16), 0, Ui.dp(c, 8), 0);
        mid.addView(Ui.text(c, title, 19, R.color.fin_text, true));
        mid.addView(Ui.text(c, sub, 15, R.color.fin_text_dim, false));
        row.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        ImageView chev = new ImageView(c);
        chev.setImageResource(R.drawable.ic_chevron_right);
        chev.setColorFilter(Ui.color(c, R.color.fin_text_dim));
        row.addView(chev, new LinearLayout.LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24)));
        return row;
    }

    private void loadSample() {
        FinanceStore.ImportRec rec = newRec("Sample data");
        int added = store.addAll(SampleData.generate(8), rec);
        FinanceState.reset();
        Toast.makeText(requireContext(), added > 0 ? "Loaded " + added + " sample transactions" : "Sample data is already loaded", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private FinanceStore.ImportRec newRec(String name) {
        FinanceStore.ImportRec rec = new FinanceStore.ImportRec();
        rec.id = UUID.randomUUID().toString();
        rec.name = name;
        rec.time = System.currentTimeMillis();
        return rec;
    }

    private String displayName(Uri uri) {
        try (Cursor cur = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return cur.getString(i);
            }
        } catch (Exception ignored) {
        }
        return "import";
    }

    private void importFile(final Uri uri, final int kind) {
        final Context app = requireContext().getApplicationContext();
        final String name = displayName(uri);
        Toast.makeText(app, "Reading " + name + "…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            String error = null;
            List<FinTxn> parsed = null;
            try (InputStream in = app.getContentResolver().openInputStream(uri)) {
                if (in == null) throw new ImportParsers.ParseException("Could not open the file.");
                switch (kind) {
                    case CSV: parsed = ImportParsers.parseCsv(in); break;
                    case JSON: parsed = ImportParsers.parseJson(in); break;
                    case TAKEOUT: parsed = ImportParsers.parseTakeout(in); break;
                    default: parsed = ImportParsers.parsePdf(app, in); break;
                }
            } catch (ImportParsers.ParseException e) {
                error = e.getMessage();
            } catch (Throwable e) {
                error = "Could not read this file (" + e.getClass().getSimpleName() + ").";
            }
            final String err = error;
            final List<FinTxn> result = parsed;
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (err != null || result == null) {
                    Toast.makeText(app, err == null ? "Nothing to import" : err, Toast.LENGTH_LONG).show();
                    return;
                }
                int added = store.addAll(result, newRec(name));
                FinanceState.reset();
                int skipped = result.size() - added;
                Toast.makeText(app, "Imported " + added + " transactions" + (skipped > 0 ? " (" + skipped + " duplicates skipped)" : ""), Toast.LENGTH_LONG).show();
                refresh();
            });
        });
    }
}
