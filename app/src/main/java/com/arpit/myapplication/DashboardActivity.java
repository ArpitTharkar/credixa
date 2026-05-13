package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {
    private Button btnWallet, btnTracking, btnSplit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ImageButton btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnWallet = findViewById(R.id.buttonWallet);
        btnTracking = findViewById(R.id.buttonTracking);
        btnSplit = findViewById(R.id.buttonSplit);

        btnWallet.setOnClickListener(v -> {
            // Open Wallet functions page
            startActivity(new Intent(this, WalletFunctionActivity.class));
        });
        btnTracking.setOnClickListener(v -> startActivity(new Intent(this, TrackingActivity.class)));
        btnSplit.setOnClickListener(v -> startActivity(new Intent(this, SplitActivity.class)));
    }
}
