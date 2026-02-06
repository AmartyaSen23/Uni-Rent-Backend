package com.campusshare.backend.repository;

import com.campusshare.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    
    // 🔥 NEW: Delete zombies
    long deleteByPasswordAndOtpGeneratedTimeBefore(String password, LocalDateTime cutoff);
}