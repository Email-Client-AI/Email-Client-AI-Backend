package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.request.*;
import com.finalproject.example.EmailClientAI.dto.response.ApiResponse;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.dto.response.IntrospectResponse;
import com.finalproject.example.EmailClientAI.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/register")
    public ApiResponse<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.register(request))
                .message("User registered successfully")
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.login(request))
                .message("Login successful")
                .build();
    }

    @PostMapping("/google")
    public ApiResponse<AuthenticationResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.loginWithGoogle(request))
                .message("Google login successful")
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.refresh(request))
                .message("Token refreshed successfully")
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        return ApiResponse.<IntrospectResponse>builder()
                .data(authenticationService.introspect(request))
                .build();
    }
}
