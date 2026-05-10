package com.arpit.walletbackend.api;

import com.arpit.walletbackend.wallet.WalletAccount;
import com.arpit.walletbackend.wallet.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<?> getBalance(@RequestParam("userId") Long userId) {
        WalletAccount wallet = walletService.findByUserId(userId).orElse(null);
        if (wallet == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("balance", wallet.getBalance()));
    }

    @PostMapping("/add-money")
    public ResponseEntity<?> addMoney(@RequestParam("userId") Long userId, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        WalletAccount w = walletService.addMoney(userId, amount);
        return ResponseEntity.ok(Map.of("balance", w.getBalance()));
    }
}
