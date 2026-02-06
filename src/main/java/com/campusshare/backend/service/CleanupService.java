package com.campusshare.backend.service;

import com.campusshare.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CleanupService {

    @Autowired
    private UserRepository userRepository;

    // Runs every day at midnight (00:00)
    // cron = "second minute hour day month weekday"
    @Scheduled(cron = "0 0 0 * * ?") 
    @Transactional // Required for delete operations
    public void deleteZombieUsers() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(10);
        
        // Custom method we will add to Repository next
        long deletedCount = userRepository.deleteByPasswordAndOtpGeneratedTimeBefore("temp_pending", cutoffDate);
        
        if (deletedCount > 0) {
            System.out.println("🧹 Cleanup: Removed " + deletedCount + " zombie accounts.");
        }
    }
}