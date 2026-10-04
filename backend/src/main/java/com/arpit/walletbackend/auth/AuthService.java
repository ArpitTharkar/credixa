package com.arpit.walletbackend.auth;

import com.arpit.walletbackend.auth.dto.AuthBridgeResponse;
import com.arpit.walletbackend.auth.dto.LoginRequest;
import com.arpit.walletbackend.auth.dto.RequestOtpResponse;
import com.arpit.walletbackend.auth.dto.RegisterRequest;
import com.arpit.walletbackend.user.AppUser;
import com.arpit.walletbackend.user.AppUserRepository;
import com.arpit.walletbackend.wallet.WalletAccount;
import com.arpit.walletbackend.wallet.WalletAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    /** Demo mode: every registration accepts this fixed OTP (no SMS provider). */
    private static final String DEMO_OTP = "123456";
    public AuthService(AppUserRepository appUserRepository, WalletAccountRepository walletAccountRepository) {
        this.appUserRepository = appUserRepository;
        this.walletAccountRepository = walletAccountRepository;
    }

    /** Phase 2: used after Firebase OTP verifies the phone; no password needed. */
    @Transactional
    public AuthBridgeResponse createOrGetUserByPhone(String rawPhone) {
        String phone = normalizePhone(rawPhone);

        AppUser existing = appUserRepository.findByPhone(phone).orElse(null);
        if (existing != null) {
            WalletAccount wallet = walletAccountRepository.findByUserId(existing.getId())
                    .orElseGet(() -> createWallet(existing));
            return new AuthBridgeResponse(existing.getId(), existing.getPhone(), wallet.getBalance(), false);
        }

        AppUser user = new AppUser();
        user.setPhone(phone);
        AppUser savedUser = appUserRepository.save(user);

        WalletAccount wallet = createWallet(savedUser);
        return new AuthBridgeResponse(savedUser.getId(), savedUser.getPhone(), wallet.getBalance(), true);
    }

    @Transactional
    public AuthBridgeResponse register(RegisterRequest req) {
        String phone = normalizePhone(req.getPhone());
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String email = req.getEmail() == null ? "" : req.getEmail().trim();

        if (appUserRepository.findByPhone(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        }
        if (appUserRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (req.getAge() == null || req.getAge() < 18) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Age must be 18 or above");
        }

        if (!DEMO_OTP.equals(req.getOtp())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        AppUser user = new AppUser();
        user.setPhone(phone);
        user.setUsername(username);
        user.setEmail(email);
        user.setAge(req.getAge());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        AppUser savedUser = appUserRepository.save(user);

        WalletAccount wallet = createWallet(savedUser);
        return new AuthBridgeResponse(savedUser.getId(), savedUser.getPhone(), wallet.getBalance(), true);
    }

    @Transactional(readOnly = true)
    public AuthBridgeResponse login(LoginRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        AppUser user = appUserRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id or password"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id or password");
        }

        WalletAccount wallet = walletAccountRepository.findByUserId(user.getId())
                .orElseGet(() -> createWallet(user));
        return new AuthBridgeResponse(user.getId(), user.getPhone(), wallet.getBalance(), false);
    }

    public RequestOtpResponse requestRegistrationOtp(String rawPhone) {
        String phone = normalizePhone(rawPhone);
        if (appUserRepository.findByPhone(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        }

        return new RequestOtpResponse("OTP sent to phone", DEMO_OTP);
    }

    private WalletAccount createWallet(AppUser user) {
        WalletAccount wallet = new WalletAccount();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        return walletAccountRepository.save(wallet);
    }

    private String normalizePhone(String phone) {
        String cleaned = phone == null ? "" : phone.replaceAll("[^0-9+]", "").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        if (cleaned.matches("^[0-9]{10}$")) {
            // Default local 10-digit numbers to India country code for consistency with app sign-up.
            return "+91" + cleaned;
        }
        if (!cleaned.startsWith("+")) {
            return "+" + cleaned;
        }
        return cleaned;
    }
}
