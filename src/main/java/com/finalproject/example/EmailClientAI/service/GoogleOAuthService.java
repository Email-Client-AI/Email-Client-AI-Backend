package com.finalproject.example.EmailClientAI.service;

import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.dto.request.GoogleLoginRequest;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;

public interface GoogleOAuthService {
    AuthenticationDTO exchangeCodeAndLogin(GoogleLoginRequest request);
}
