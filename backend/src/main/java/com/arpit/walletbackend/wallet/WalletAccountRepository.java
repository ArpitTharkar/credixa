package com.arpit.walletbackend.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {
	Optional<WalletAccount> findByUserId(Long userId);
}
