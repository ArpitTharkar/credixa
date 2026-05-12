package com.arpit.walletbackend.api;

import com.arpit.walletbackend.tx.WalletTransaction;
import com.arpit.walletbackend.wallet.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class TransferController {

    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> body) {
        Long sender = Long.valueOf(body.get("senderUserId").toString());
        Long receiver = Long.valueOf(body.get("receiverUserId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String idempotencyKey = body.get("idempotencyKey") == null ? null : body.get("idempotencyKey").toString();

        try {
            WalletTransaction tx = walletService.transfer(sender, receiver, amount, idempotencyKey);
            return ResponseEntity.ok(Map.of("transactionId", tx.getId(), "status", tx.getStatus()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
