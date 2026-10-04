package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
            empty.setTextColor(getColor(R.color.fin_text_dim));
            empty.setTextSize(16);
            empty.setPadding(16, 24, 16, 8);
            container.addView(empty);
            return;
        }
        for (WalletRepository.Transaction tx : list) {
            View row = getLayoutInflater().inflate(R.layout.item_fin_txn, container, false);
            boolean credit = "ADD".equals(tx.type) || "RECEIVED".equals(tx.type);
            String label = "SEND".equals(tx.type) ? "Sent" : "ADD".equals(tx.type) ? "Added to wallet"
                    : "RECEIVED".equals(tx.type) ? "Received" : tx.type;
            ((TextView) row.findViewById(R.id.txnTitle)).setText(label);
            ((TextView) row.findViewById(R.id.txnSub)).setText(DateFormat.getDateTimeInstance().format(new Date(tx.time)));
            TextView amount = row.findViewById(R.id.txnAmount);
            amount.setText((credit ? "+" : "-") + "₹" + tx.amount);
            amount.setTextColor(getColor(credit ? R.color.fin_green_text : R.color.fin_text));
            ImageView icon = row.findViewById(R.id.txnIcon);
            icon.setImageResource(credit ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
            icon.setColorFilter(getColor(credit ? R.color.fin_green_text : R.color.fin_red));
            row.findViewById(R.id.txnIconBg).setBackgroundResource(credit ? R.drawable.bg_circle_green : R.drawable.bg_circle_red);
            container.addView(row);
        }
    }
}
