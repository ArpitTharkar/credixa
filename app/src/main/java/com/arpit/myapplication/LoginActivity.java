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
import com.arpit.myapplication.remote.model.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login with phone + password.
 * Registration is handled by SignInActivity.
 * TODO (Phase 2): add Firebase OTP verification on top of this flow.
 */
public class LoginActivity extends AppCompatActivity {
    private EditText etUserId;
    private EditText etPassword;
    private Button btnLogin;
    private UserRepository userRepo;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUserId = findViewById(R.id.editTextLoginUsername);
        etPassword = findViewById(R.id.editTextLoginPassword);
        btnLogin = findViewById(R.id.buttonLogin);
        ImageButton btnBack = findViewById(R.id.buttonBack);
        TextView tvForgot = findViewById(R.id.textViewForgot);

        userRepo = new UserRepository(this);
        backendApi = ApiClient.backendApi();

        btnLogin.setOnClickListener(v -> loginWithBackend());

        btnBack.setOnClickListener(v -> {
            finishAffinity();
        });

        tvForgot.setOnClickListener(v -> {
            startActivity(new Intent(this, SignInActivity.class));
        });
    }

    private void loginWithBackend() {
        String userId = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (userId.isEmpty()) {
            Toast.makeText(this, "Enter user id", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        Call<AuthBridgeResponse> call = backendApi.login(new LoginRequest(userId, password));
        call.enqueue(new Callback<AuthBridgeResponse>() {
            @Override
            public void onResponse(Call<AuthBridgeResponse> call, Response<AuthBridgeResponse> response) {
                btnLogin.setEnabled(true);
                if (response.code() == 401) {
                    Toast.makeText(LoginActivity.this, "Invalid user id or password", Toast.LENGTH_SHORT).show();
                    return;
                }
                AuthBridgeResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Toast.makeText(LoginActivity.this, "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                userRepo.setBackendUserId(body.getUserId());
                userRepo.setCurrentUser(body.getPhone());
                userRepo.addUser(body.getPhone(), body.getPhone());

                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<AuthBridgeResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "Cannot reach server. Start the backend and try again.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

}
