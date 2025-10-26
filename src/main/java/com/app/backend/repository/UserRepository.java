package com.app.backend.repository;

import com.app.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    @Transactional
    void deleteByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedAttempts = 0, u.accountLockedUntil = NULL WHERE u.username = :username")
    void resetLockState(String username);
}
