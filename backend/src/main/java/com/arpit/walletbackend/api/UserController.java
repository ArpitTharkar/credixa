package com.arpit.walletbackend.api;

import com.arpit.walletbackend.user.AppUser;
import com.arpit.walletbackend.user.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository appUserRepository;

    public UserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/resolve")
    public ResponseEntity<?> resolve(@RequestParam("identifier") String identifier) {
        String raw = identifier == null ? "" : identifier.trim();
        if (raw.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Identifier is required"));
        }

        AppUser user = resolveUser(raw);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "phone", user.getPhone() == null ? "" : user.getPhone(),
                "username", user.getUsername() == null ? "" : user.getUsername()
        ));
    }

    private AppUser resolveUser(String raw) {
        String normalizedPhone = normalizePhone(raw);
        AppUser byPhone = appUserRepository.findByPhone(normalizedPhone).orElse(null);
        if (byPhone != null) {
            return byPhone;
        }

        if (raw.chars().allMatch(Character::isDigit)) {
            try {
                Long id = Long.valueOf(raw);
                AppUser byId = appUserRepository.findById(id).orElse(null);
                if (byId != null) {
                    return byId;
                }
            } catch (NumberFormatException ignored) {
                // Fall through to username lookup.
            }
        }

        return appUserRepository.findByUsername(raw).orElse(null);
    }

    private String normalizePhone(String phone) {
        String cleaned = phone == null ? "" : phone.replaceAll("[^0-9+]", "").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        if (cleaned.matches("^[0-9]{10}$")) {
            return "+91" + cleaned;
        }
        if (!cleaned.startsWith("+")) {
            return "+" + cleaned;
        }
        return cleaned;
    }
}
