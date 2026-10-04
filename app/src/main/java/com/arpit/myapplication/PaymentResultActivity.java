package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentResultActivity extends AppCompatActivity {

    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_AMOUNT  = "amount";
    public static final String EXTRA_TO      = "to_user";
        public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_REASON  = "reason";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_result);

        boolean success = getIntent().getBooleanExtra(EXTRA_SUCCESS, false);
        long    amount  = getIntent().getLongExtra(EXTRA_AMOUNT, 0);
        String  toUser  = getIntent().getStringExtra(EXTRA_TO);
        String  reason  = getIntent().getStringExtra(EXTRA_REASON);
        String  category = getIntent().getStringExtra(EXTRA_CATEGORY);

        TextView iconView   = findViewById(R.id.textViewResultIcon);
        TextView titleView  = findViewById(R.id.textViewResultTitle);
        TextView amountView = findViewById(R.id.textViewResultAmount);
        TextView toView     = findViewById(R.id.textViewResultTo);
        TextView timeView   = findViewById(R.id.textViewResultTime);
        TextView reasonView = findViewById(R.id.textViewResultReason);
        TextView categoryView = findViewById(R.id.textViewResultCategory);
        Button   btnDone    = findViewById(R.id.buttonResultDone);

        if (success) {
            iconView.setText("✔");
            iconView.setBackgroundResource(R.drawable.bg_circle_success);
            titleView.setText("Payment successful");
            titleView.setTextColor(getColor(R.color.fin_green_text));
        } else {
            iconView.setText("✗");
            iconView.setBackgroundResource(R.drawable.bg_circle_fail);
            titleView.setText("Payment failed");
            titleView.setTextColor(getColor(R.color.fin_red));
        }

        amountView.setText("₹" + amount);
        toView.setText(toUser != null ? toUser : "-");
        if (category != null && !category.isEmpty()) {
            categoryView.setText(category);
        }
        timeView.setText(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));

        if (reason != null && !reason.isEmpty()) {
            reasonView.setVisibility(View.VISIBLE);
            reasonView.setText(reason);
        }

        // Done → go back to Dashboard, clear stack above it
        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, FinanceActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Back (top-left) → go to WalletFunctionActivity
        ImageButton btnBack = findViewById(R.id.buttonBack);
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }
}
