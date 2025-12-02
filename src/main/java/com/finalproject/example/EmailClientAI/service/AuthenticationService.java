package com.finalproject.example.EmailClientAI.service;

import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.dto.request.IntrospectRequest;
import com.finalproject.example.EmailClientAI.dto.request.LoginRequest;
import com.finalproject.example.EmailClientAI.dto.request.RefreshTokenRequest;
import com.finalproject.example.EmailClientAI.dto.request.RegisterRequest;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.dto.response.IntrospectResponse;
import org.springframework.http.ResponseCookie;

public interface AuthenticationService {

    void logout(String refreshToken, String deviceId);

    AuthenticationDTO refresh(String refreshToken, String deviceId);

    ResponseCookie createHttpOnlyCookie(String name, String value);
}
