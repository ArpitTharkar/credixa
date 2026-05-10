package com.arpit.walletbackend.tx;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
	Optional<WalletTransaction> findByIdempotencyKey(String key);
	List<WalletTransaction> findBySenderUserIdOrReceiverUserIdOrderByCreatedAtDesc(Long senderId, Long receiverId);
}
