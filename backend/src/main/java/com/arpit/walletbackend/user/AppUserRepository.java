package com.arpit.walletbackend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByPhone(String phone);
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findById(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
