package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.entity.UserSession;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.UserSessionRepository;
import com.finalproject.example.EmailClientAI.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserSessionRepository userSessionRepository;
    @Override
    public UserSession findActiveSession(UUID userId) {
        var activeSession =  userSessionRepository.findTopByUserIdOrderByExpiresAtDesc(userId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        if (activeSession.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }
        return activeSession;
    }
}
