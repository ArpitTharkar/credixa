package com.arpit.walletbackend.wallet;

import com.arpit.walletbackend.tx.TransactionStatus;
import com.arpit.walletbackend.tx.WalletTransaction;
import com.arpit.walletbackend.tx.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class WalletService {

    private final WalletAccountRepository walletRepo;
    private final WalletTransactionRepository txRepo;

    public WalletService(WalletAccountRepository walletRepo, WalletTransactionRepository txRepo) {
        this.walletRepo = walletRepo;
        this.txRepo = txRepo;
    }

    public Optional<WalletAccount> findByUserId(Long userId) {
        return walletRepo.findByUserId(userId);
    }

    @Transactional
    public WalletAccount addMoney(Long userId, BigDecimal amount) {
        WalletAccount wallet = walletRepo.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("User wallet not found"));
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepo.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setSenderUserId(userId);
        tx.setReceiverUserId(userId);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.SUCCESS);
        txRepo.save(tx);

        return wallet;
    }

    @Transactional
    public WalletTransaction transfer(Long senderUserId, Long receiverUserId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<WalletTransaction> existing = txRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) return existing.get();
        }

        WalletTransaction tx = new WalletTransaction();
        tx.setSenderUserId(senderUserId);
        tx.setReceiverUserId(receiverUserId);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.INITIATED);
        tx.setIdempotencyKey(idempotencyKey);
        tx = txRepo.save(tx);

        WalletAccount sender = walletRepo.findByUserId(senderUserId).orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        WalletAccount receiver = walletRepo.findByUserId(receiverUserId).orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (sender.getBalance().compareTo(amount) < 0) {
            tx.setStatus(TransactionStatus.FAILED);
            txRepo.save(tx);
            throw new IllegalStateException("Insufficient funds");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        walletRepo.save(sender);
        walletRepo.save(receiver);

        tx.setStatus(TransactionStatus.SUCCESS);
        txRepo.save(tx);
        return tx;
    }

    public List<WalletTransaction> historyForUser(Long userId) {
        return txRepo.findBySenderUserIdOrReceiverUserIdOrderByCreatedAtDesc(userId, userId);
    }
}
