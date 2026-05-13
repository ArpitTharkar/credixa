package com.arpit.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class TransactionsActivity extends AppCompatActivity {
    private LinearLayout container;
    private WalletViewModel walletViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        ImageButton btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(this, WalletFunctionActivity.class);
            startActivity(i);
            finish();
        });

        walletViewModel = new ViewModelProvider(this).get(WalletViewModel.class);
        container = findViewById(R.id.transactionsContainer);

        // Observe transactions LiveData
        walletViewModel.transactions.observe(this, this::renderTransactions);

        walletViewModel.refreshTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning to this screen so incoming transfers appear immediately.
        walletViewModel.refreshTransactions();
    }

    private void renderTransactions(List<WalletRepository.Transaction> list) {
        container.removeAllViews();
        if (list == null || list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No transactions yet");
            empty.setPadding(16, 24, 16, 8);
            container.addView(empty);
            return;
        }
        for (WalletRepository.Transaction tx : list) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            tv.setLayoutParams(lp);
            tv.setPadding(16, 12, 16, 12);
            tv.setTextSize(15);

            String label;
            String sign;
            int color;
            if ("SEND".equals(tx.type)) {
                label = "Sent"; sign = "-"; color = 0xFFE53935;
            } else if ("ADD".equals(tx.type)) {
                label = "Added"; sign = "+"; color = 0xFF43A047;
            } else if ("RECEIVED".equals(tx.type)) {
                label = "Received"; sign = "+"; color = 0xFF43A047;
            } else {
                label = tx.type; sign = ""; color = 0xFF212121;
            }

            String time = DateFormat.getDateTimeInstance().format(new Date(tx.time));
            tv.setText(sign + "₹" + tx.amount + "  " + label + "\n" + time);
            tv.setTextColor(color);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setBackgroundColor(0xFFF5F5F5);
            container.addView(tv);
        }
    }
}
