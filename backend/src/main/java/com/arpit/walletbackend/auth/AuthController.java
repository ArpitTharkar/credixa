package com.arpit.walletbackend.auth;

import com.arpit.walletbackend.auth.dto.AuthBridgeRequest;
import com.arpit.walletbackend.auth.dto.AuthBridgeResponse;
import com.arpit.walletbackend.auth.dto.LoginRequest;
import com.arpit.walletbackend.auth.dto.RequestOtpRequest;
import com.arpit.walletbackend.auth.dto.RequestOtpResponse;
import com.arpit.walletbackend.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/phone-login")
    public ResponseEntity<AuthBridgeResponse> phoneLogin(@Valid @RequestBody AuthBridgeRequest request) {
        return ResponseEntity.ok(authService.createOrGetUserByPhone(request.getPhone()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthBridgeResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/register/request-otp")
    public ResponseEntity<RequestOtpResponse> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        return ResponseEntity.ok(authService.requestRegistrationOtp(request.getPhone()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthBridgeResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
