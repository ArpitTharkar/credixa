package com.arpit.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class SendMoneyActivity extends AppCompatActivity {
    private EditText editReceiver, editAmount;
    private Button btnSend;
    private TextView textCategory;
    private String selectedCategory = null;
    private WalletViewModel walletViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        walletViewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        editReceiver = findViewById(R.id.editTextReceiver);
        textCategory = findViewById(R.id.textViewCategory);
        editAmount   = findViewById(R.id.editTextSendAmount);
        editAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        btnSend = findViewById(R.id.buttonSendMoneyNow);

        // optional prefill from split flow
        String preReceiver = getIntent().getStringExtra("prefill_receiver");
        long preAmount = getIntent().getLongExtra("prefill_amount", 0L);
        if (preReceiver != null && !preReceiver.isEmpty()) {
            editReceiver.setText(preReceiver);
        }
        if (preAmount > 0) {
            editAmount.setText(String.valueOf(preAmount));
        }

        btnSend.setOnClickListener(v -> performSend());

        textCategory.setOnClickListener(v -> {
            final String[] items = new String[]{"Food", "Travel", "Shopping", "Rent", "Other"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select category");
            builder.setItems(items, (dialog, which) -> {
                selectedCategory = items[which];
                textCategory.setText(selectedCategory);
            });
            builder.show();
        });
    }

    private void performSend() {
        String receiverInput = editReceiver.getText().toString().trim();
        String sAmount       = editAmount.getText().toString().trim();

        if (receiverInput.isEmpty()) {
            Toast.makeText(this, "Phone/User ID required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sAmount.isEmpty()) {
            Toast.makeText(this, "Amount required", Toast.LENGTH_SHORT).show();
            return;
        }

        long amount = Long.parseLong(sAmount);

        // Show processing dialog
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_processing, null);
        TextView tv = dialogView.findViewById(R.id.textViewProcessing);
        tv.setText("Processing payment…");
        b.setView(dialogView);
        b.setCancelable(false);
        AlertDialog dlg = b.create();
        dlg.show();

        // Observe result ONCE — after delay via Handler to mimic real processing
        new Handler().postDelayed(() -> {
            // ViewModel handles all validation + transfer logic
            walletViewModel.sendResult.observe(this, result -> {
                if (result == null) return;
                dlg.dismiss();

                // Navigate to result screen
                Intent intent = new Intent(this, PaymentResultActivity.class);
                intent.putExtra(PaymentResultActivity.EXTRA_SUCCESS, result.success);
                intent.putExtra(PaymentResultActivity.EXTRA_AMOUNT,  result.amount);
                intent.putExtra(PaymentResultActivity.EXTRA_TO,      result.toUser);
                        // pass selected category to result screen
                        intent.putExtra(PaymentResultActivity.EXTRA_CATEGORY, selectedCategory != null ? selectedCategory : "Other");
                if (result.reason != null) {
                    intent.putExtra(PaymentResultActivity.EXTRA_REASON, result.reason);
                }
                walletViewModel.resetSendResult();
                startActivity(intent);
                finish();
            });

                // pass selected category (or null) to ViewModel
                walletViewModel.sendMoney(receiverInput, amount, selectedCategory);
        }, 1500);
    }
}

