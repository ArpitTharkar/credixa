package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class WalletFunctionActivity extends AppCompatActivity {
    private Button btnCheckBalance, btnAddMoney, btnTransaction, btnSendMoney;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_function);

        ImageButton btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });

        btnCheckBalance = findViewById(R.id.buttonCheckBalance);
        btnAddMoney = findViewById(R.id.buttonAddMoney);
        btnTransaction = findViewById(R.id.buttonTransaction);
        btnSendMoney = findViewById(R.id.buttonSendMoney);

        btnCheckBalance.setOnClickListener(v -> startActivity(new Intent(this, CheckBalanceActivity.class)));
        btnAddMoney.setOnClickListener(v -> startActivity(new Intent(this, AddMoneyActivity.class)));
        btnTransaction.setOnClickListener(v -> startActivity(new Intent(this, TransactionsActivity.class)));
        btnSendMoney.setOnClickListener(v -> startActivity(new Intent(this, SendMoneyActivity.class)));
    }
}
