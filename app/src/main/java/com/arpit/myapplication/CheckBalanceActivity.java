package com.arpit.myapplication;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class CheckBalanceActivity extends AppCompatActivity {
    private TextView textViewBalance;
    private WalletViewModel walletViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_balance);

        walletViewModel = new ViewModelProvider(this).get(WalletViewModel.class);
        textViewBalance = findViewById(R.id.textViewBalancePage);

        // Observe balance LiveData
        walletViewModel.balance.observe(this, bal -> {
            if (bal == null) {
                textViewBalance.setText("Loading...");
            } else {
                textViewBalance.setText("Balance: ₹" + bal);
            }
        });

        textViewBalance.setText("Loading...");
        walletViewModel.refreshBalance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning to this screen so cross-device transfers are reflected.
        walletViewModel.refreshBalance();
    }
}
