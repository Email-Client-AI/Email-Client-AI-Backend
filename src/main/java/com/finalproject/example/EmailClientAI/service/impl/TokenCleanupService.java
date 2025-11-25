package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.repository.InvalidatedTokenRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenCleanupService {

    InvalidatedTokenRepository invalidatedTokenRepository;

    @Scheduled(fixedDelay = 86400000) // Run every 24 hours (86400000 ms)
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired tokens...");
        invalidatedTokenRepository.deleteAllByExpirationTimeBefore(LocalDateTime.now());
        log.info("Expired tokens cleanup completed.");
    }
}
