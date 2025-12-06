package com.finalproject.example.EmailClientAI.service;

import com.finalproject.example.EmailClientAI.entity.UserSession;

import java.util.UUID;

public interface UserService {
    UserSession findActiveSession(UUID userId);
}
