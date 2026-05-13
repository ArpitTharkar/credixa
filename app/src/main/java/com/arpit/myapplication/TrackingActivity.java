package com.arpit.myapplication;

import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.EditText;
import android.text.InputType;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackingActivity extends AppCompatActivity {

    private WalletViewModel walletViewModel;
    private LinearLayout container;
    private TextView totalView;
    private RadioGroup radioGroupPeriod;
    private RadioButton radioWeekly, radioMonthly;
    private List<WalletRepository.Transaction> currentTransactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        ImageButton btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });

        walletViewModel = new ViewModelProvider(this).get(WalletViewModel.class);
        container = findViewById(R.id.trackingContainer);
        totalView = findViewById(R.id.textViewTotalSpend);
        radioGroupPeriod = findViewById(R.id.radioGroupPeriod);
        radioWeekly = findViewById(R.id.radioWeekly);
        radioMonthly = findViewById(R.id.radioMonthly);

        // Track live transactions from backend via ViewModel.
        walletViewModel.transactions.observe(this, txs -> {
            currentTransactions = txs;
            computeAndDisplay(radioMonthly.isChecked());
        });

        // default show monthly
        computeAndDisplay(true);

        radioGroupPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioMonthly) computeAndDisplay(true);
            else computeAndDisplay(false);
        });

        walletViewModel.refreshTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        walletViewModel.refreshTransactions();
    }

    private void computeAndDisplay(boolean monthly) {
        String user = new UserRepository(this).getCurrentUser();
        if (user == null) return;

        WalletRepository repo = new WalletRepository(this);
        List<WalletRepository.Transaction> txs = currentTransactions;
        if (txs == null) txs = java.util.Collections.emptyList();

        long totalSpend = 0L;
        Map<String, Long> byCat = new HashMap<>();

        long now = System.currentTimeMillis();
        long startMillis;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (monthly) {
            // start of current month
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            startMillis = cal.getTimeInMillis();
        } else {
            // last 7 days
            cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
            startMillis = cal.getTimeInMillis();
        }

        for (WalletRepository.Transaction t : txs) {
            boolean isSend = "SEND".equalsIgnoreCase(t.type);
            boolean isSuccess = (t.status == null || t.status.isEmpty()) || "SUCCESS".equalsIgnoreCase(t.status);
            boolean inRange = t.time >= startMillis && t.time <= now;
            if (isSend && isSuccess && inRange) {
                totalSpend += t.amount;
                String c = t.category != null ? t.category : "Other";
                byCat.put(c, byCat.getOrDefault(c, 0L) + t.amount);
            }
        }

        totalView.setText("Total spending: ₹" + totalSpend);

        container.removeAllViews();
        final boolean isMonthly = monthly;
        for (Map.Entry<String, Long> e : byCat.entrySet()) {
            final String category = e.getKey();
            final long amount = e.getValue();

            View row = getLayoutInflater().inflate(R.layout.item_tracking_row, container, false);
            TextView name = row.findViewById(R.id.textViewRowName);
            TextView amt = row.findViewById(R.id.textViewRowAmount);
            TextView limitView = row.findViewById(R.id.textViewRowLimit);
            TextView warn = row.findViewById(R.id.textViewRowWarning);

            name.setText(category);
            amt.setText("₹" + amount);

            long limit = repo.getCategoryLimit(user, category);
            // show affordance always
            limitView.setVisibility(View.VISIBLE);
            if (limit > 0) {
                limitView.setText("Limit: ₹" + limit + " (tap to edit)");
                if (amount > limit) {
                    warn.setVisibility(View.VISIBLE);
                } else {
                    warn.setVisibility(View.GONE);
                }
            } else {
                limitView.setText("Tap to set limit");
                warn.setVisibility(View.GONE);
            }

            // make row clickable and open dialog to set/clear limit
            row.setClickable(true);
            row.setOnClickListener(v -> {
                EditText input = new EditText(TrackingActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setText(limit > 0 ? String.valueOf(limit) : "");
                new AlertDialog.Builder(TrackingActivity.this)
                        .setTitle("Set limit for " + category)
                        .setView(input)
                        .setPositiveButton("Save", (dialog, which) -> {
                            String s = input.getText().toString().trim();
                            long newLimit = 0L;
                            try {
                                if (!s.isEmpty()) newLimit = Long.parseLong(s);
                            } catch (NumberFormatException ignored) {}
                            repo.setCategoryLimit(user, category, newLimit);
                            computeAndDisplay(isMonthly);
                        })
                        .setNeutralButton("Clear", (dialog, which) -> {
                            repo.setCategoryLimit(user, category, 0L);
                            computeAndDisplay(isMonthly);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            container.addView(row);
        }
    }
}
