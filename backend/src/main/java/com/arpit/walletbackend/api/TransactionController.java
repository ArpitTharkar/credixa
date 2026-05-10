package com.arpit.walletbackend.api;

import com.arpit.walletbackend.tx.WalletTransaction;
import com.arpit.walletbackend.wallet.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TransactionController {

    private final WalletService walletService;

    public TransactionController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/api/transactions")
    public ResponseEntity<List<TransactionView>> history(@RequestParam("userId") Long userId) {
        List<WalletTransaction> list = walletService.historyForUser(userId);
        List<TransactionView> view = list.stream()
                .map(tx -> TransactionView.from(tx, userId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(view);
    }
}
