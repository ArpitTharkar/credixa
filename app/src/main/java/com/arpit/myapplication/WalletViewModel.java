package com.arpit.myapplication;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class WalletViewModel extends AndroidViewModel {

    private final WalletRepository walletRepo;
    private final UserRepository userRepo;

    // Balance
    private final MutableLiveData<Long> _balance = new MutableLiveData<>(0L);
    public final LiveData<Long> balance = _balance;

    // Transactions
    private final MutableLiveData<List<WalletRepository.Transaction>> _transactions = new MutableLiveData<>();
    public final LiveData<List<WalletRepository.Transaction>> transactions = _transactions;

    // Send money result: null = idle, true = success, false = failure
    public static class SendResult {
        public final boolean success;
        public final long amount;
        public final String toUser;
        public final String reason;

        public SendResult(boolean success, long amount, String toUser, String reason) {
            this.success = success;
            this.amount = amount;
            this.toUser = toUser;
            this.reason = reason;
        }
    }

    private final MutableLiveData<SendResult> _sendResult = new MutableLiveData<>(null);
    public final LiveData<SendResult> sendResult = _sendResult;

    // Add money result message
    private final MutableLiveData<String> _addMoneyMessage = new MutableLiveData<>();
    public final LiveData<String> addMoneyMessage = _addMoneyMessage;

    public WalletViewModel(@NonNull Application application) {
        super(application);
        walletRepo = ServiceLocator.provideWalletRepository();
        userRepo = ServiceLocator.provideUserRepository();
    }

    // ─── Balance ────────────────────────────────────────────────────────────────

    public void refreshBalance() {
        Long backendId = userRepo.getBackendUserId();
        if (backendId == null) { _balance.setValue(0L); return; }
        walletRepo.getBalanceAsync(String.valueOf(backendId), (ok, bal) -> {
            if (ok) _balance.postValue(bal); else _balance.postValue(0L);
        });
    }

    // ─── Add Money ──────────────────────────────────────────────────────────────

    public void addMoney(long amount) {
        Long backendId = userRepo.getBackendUserId();
        if (backendId == null) { _addMoneyMessage.setValue("Please login first"); return; }
        if (amount <= 0) { _addMoneyMessage.setValue("Amount must be greater than 0"); return; }
        walletRepo.addMoneyAsync(String.valueOf(backendId), amount, (ok, msg) -> {
            if (ok) {
                _addMoneyMessage.postValue("₹" + amount + " added successfully");
                refreshBalance();
            } else {
                _addMoneyMessage.postValue("Failed: " + msg);
            }
        });
    }

    // ─── Transactions ───────────────────────────────────────────────────────────

    public void refreshTransactions() {
        Long backendId = userRepo.getBackendUserId();
        if (backendId == null) { _transactions.setValue(null); return; }
        walletRepo.getTransactionsAsync(String.valueOf(backendId), (ok, list) -> {
            if (ok) _transactions.postValue(list); else _transactions.postValue(null);
        });
    }

    // ─── Send Money ─────────────────────────────────────────────────────────────

    public void sendMoney(String receiverInput, long amount, String category) {
        Long senderBackendId = userRepo.getBackendUserId();
        if (senderBackendId == null) {
            _sendResult.setValue(new SendResult(false, amount, receiverInput, "No user logged in"));
            return;
        }
        if (amount <= 0) {
            _sendResult.setValue(new SendResult(false, amount, receiverInput, "Amount must be greater than 0"));
            return;
        }

        walletRepo.resolveUserIdAsync(receiverInput, (resolved, receiverId, message) -> {
            if (!resolved || receiverId == null) {
                _sendResult.postValue(new SendResult(false, amount, receiverInput, message));
                return;
            }
            if (senderBackendId.equals(receiverId)) {
                _sendResult.postValue(new SendResult(false, amount, receiverInput, "Cannot send money to yourself"));
                return;
            }

            walletRepo.transferAsync(senderBackendId, receiverId, amount, (ok, msg) -> {
                if (ok) {
                    refreshBalance();
                    _sendResult.postValue(new SendResult(true, amount, receiverInput, null));
                } else {
                    _sendResult.postValue(new SendResult(false, amount, receiverInput, msg));
                }
            });
        });
    }

    public void resetSendResult() {
        _sendResult.setValue(null);
    }
}
