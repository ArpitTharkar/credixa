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
            finish();
        });

        walletViewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        editAmount = findViewById(R.id.editTextAddAmount);
        editAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        btnConfirm = findViewById(R.id.buttonConfirmAdd);

        int[] chipIds = {R.id.chipAdd100, R.id.chipAdd500, R.id.chipAdd1000, R.id.chipAdd2000};
        long[] chipAmounts = {100, 500, 1000, 2000};
        for (int i = 0; i < chipIds.length; i++) {
            final long amt = chipAmounts[i];
            findViewById(chipIds[i]).setOnClickListener(v -> {
                String cur = editAmount.getText().toString().trim();
                long base = cur.isEmpty() ? 0 : Long.parseLong(cur);
                editAmount.setText(String.valueOf(base + amt));
                editAmount.setSelection(editAmount.getText().length());
            });
        }

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
