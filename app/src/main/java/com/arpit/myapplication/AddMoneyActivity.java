package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class AddMoneyActivity extends AppCompatActivity {
    private EditText editAmount;
    private Button btnConfirm;
    private WalletViewModel walletViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_money);

        ImageButton btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(this, WalletFunctionActivity.class);
            startActivity(i);
            finish();
        });

        walletViewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        editAmount = findViewById(R.id.editTextAddAmount);
        editAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        btnConfirm = findViewById(R.id.buttonConfirmAdd);

        // Observe add money result message
        walletViewModel.addMoneyMessage.observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                if (msg.contains("added")) finish();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String s = editAmount.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }
            walletViewModel.addMoney(Long.parseLong(s));
        });
    }
}
