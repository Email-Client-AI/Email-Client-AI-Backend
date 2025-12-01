package com.finalproject.example.EmailClientAI.service;

import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.dto.GoogleOAuthDTO;
import com.finalproject.example.EmailClientAI.dto.request.GoogleLoginRequest;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.entity.UserSession;

public interface GoogleOAuthService {
    AuthenticationDTO exchangeCodeAndLogin(GoogleLoginRequest request);
    GoogleOAuthDTO refreshAccessToken(String refreshToken);
    UserSession generateUserSessionWithTokens(User user, String googleAccessToken, String googleRefreshToken, String appRefreshToken, Long refreshTokenDuration);
}
