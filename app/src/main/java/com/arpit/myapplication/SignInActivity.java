package com.arpit.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arpit.myapplication.remote.ApiClient;
import com.arpit.myapplication.remote.BackendApi;
import com.arpit.myapplication.remote.model.AuthBridgeResponse;
import com.arpit.myapplication.remote.model.RequestOtpRequest;
import com.arpit.myapplication.remote.model.RequestOtpResponse;
import com.arpit.myapplication.remote.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registration screen: phone + password.
 * TODO (Phase 2): add Firebase OTP verification before creating account.
 */
public class SignInActivity extends AppCompatActivity {
    private EditText etUserId, etEmail, etAge, etPhone, etOtp, etPassword, etRePassword;
    private Button btnSendOtp;
    private Button btnRegister;
    private TextView btnGoToLogin;
    private UserRepository userRepo;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        etUserId = findViewById(R.id.editTextSignUsername);
        etEmail = findViewById(R.id.editTextSignEmail);
        etAge = findViewById(R.id.editTextSignAge);
        etPhone = findViewById(R.id.editTextSignPhone);
        etOtp = findViewById(R.id.editTextSignOtp);
        etPassword = findViewById(R.id.editTextSignPassword);
        etRePassword = findViewById(R.id.editTextSignRePassword);
        btnSendOtp = findViewById(R.id.buttonSendOtp);
        btnRegister = findViewById(R.id.buttonRegister);
        btnGoToLogin = (TextView) findViewById(R.id.buttonGoToLogin);
        ImageButton btnBack = findViewById(R.id.buttonBack);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        userRepo = new UserRepository(this);
        backendApi = ApiClient.backendApi();

        btnSendOtp.setOnClickListener(v -> requestOtp());

        btnRegister.setOnClickListener(v -> {
            registerWithBackend();
        });
    }

    private void requestOtp() {
        String phone = normalizePhone(etPhone.getText().toString().trim());
        if (phone == null) {
            Toast.makeText(this, "Enter valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendOtp.setEnabled(false);
        backendApi.requestRegistrationOtp(new RequestOtpRequest(phone)).enqueue(new Callback<RequestOtpResponse>() {
            @Override
            public void onResponse(Call<RequestOtpResponse> call, Response<RequestOtpResponse> response) {
                btnSendOtp.setEnabled(true);
                if (response.code() == 409) {
                    Toast.makeText(SignInActivity.this, "Phone already registered. Please log in.", Toast.LENGTH_LONG).show();
                    return;
                }
                RequestOtpResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Toast.makeText(SignInActivity.this, "Could not send OTP", Toast.LENGTH_SHORT).show();
                    return;
                }
                String otp = body.getOtpForDev() == null ? "" : body.getOtpForDev();
                Toast.makeText(SignInActivity.this, "OTP sent. Dev OTP: " + otp, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<RequestOtpResponse> call, Throwable t) {
                btnSendOtp.setEnabled(true);
                String reason = t.getMessage() == null ? "unknown network error" : t.getMessage();
                Toast.makeText(SignInActivity.this,
                        "Cannot reach server: " + reason,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerWithBackend() {
        String userId = etUserId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String ageText = etAge.getText().toString().trim();
        String phone    = normalizePhone(etPhone.getText().toString().trim());
        String otp = etOtp.getText().toString().trim();
        String pass     = etPassword.getText().toString();
        String rePass   = etRePassword.getText().toString();

        int age;

        if (userId.isEmpty() || email.isEmpty() || ageText.isEmpty()) {
            Toast.makeText(this, "User id, email and age are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone == null) {
            Toast.makeText(this, "Enter valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Enter valid age", Toast.LENGTH_SHORT).show();
            return;
        }
        if (age < 18) {
            Toast.makeText(this, "Age must be 18 or above", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(rePass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!otp.matches("^[0-9]{6}$")) {
            Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        Call<AuthBridgeResponse> call = backendApi.register(
                new RegisterRequest(userId, email, age, phone, pass, otp)
        );
        call.enqueue(new Callback<AuthBridgeResponse>() {
            @Override
            public void onResponse(Call<AuthBridgeResponse> call, Response<AuthBridgeResponse> response) {
                btnRegister.setEnabled(true);
                if (response.code() == 409) {
                    Toast.makeText(SignInActivity.this,
                            "Phone/User ID/Email already exists.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (response.code() == 400) {
                    Toast.makeText(SignInActivity.this,
                            "Invalid or expired OTP. Request OTP again.", Toast.LENGTH_LONG).show();
                    return;
                }
                AuthBridgeResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Toast.makeText(SignInActivity.this, "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                userRepo.setBackendUserId(body.getUserId());
                userRepo.setCurrentUser(body.getPhone());
                userRepo.addUser(body.getPhone(), body.getPhone());

                Toast.makeText(SignInActivity.this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignInActivity.this, DashboardActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<AuthBridgeResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                String reason = t.getMessage() == null ? "unknown network error" : t.getMessage();
                Toast.makeText(SignInActivity.this,
                        "Cannot reach server: " + reason,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim().replace(" ", "");
        if (trimmed.isEmpty()) return null;
        if (trimmed.matches("^[0-9]{10}$")) return "+91" + trimmed;
        if (trimmed.matches("^\\+[1-9][0-9]{7,14}$")) return trimmed;
        return null;
    }
}
