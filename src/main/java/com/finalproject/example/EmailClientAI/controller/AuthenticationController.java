package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.dto.request.*;
import com.finalproject.example.EmailClientAI.dto.response.ApiResponse;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.dto.response.IntrospectResponse;
import com.finalproject.example.EmailClientAI.service.GoogleOAuthService;
import com.finalproject.example.EmailClientAI.service.impl.AuthenticationService;
import com.finalproject.example.EmailClientAI.utils.Utils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final GoogleOAuthService googleOAuthService;

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
    public ResponseEntity<AuthenticationDTO> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        var authenticationDTO = googleOAuthService.exchangeCodeAndLogin(request);
        var refreshCookie = Utils.createHttpOnlyCookie("refresh_token", authenticationDTO.getRefreshToken());
        var deviceCookie = Utils.createHttpOnlyCookie("device_id", authenticationDTO.getDeviceId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deviceCookie.toString())
                .body(authenticationDTO);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(authenticationService.refresh(request))
                .message("Token refreshed successfully")
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token") String refreshToken,
                                       @CookieValue(name = "device_id") String deviceId) {
        authenticationService.logout(refreshToken, deviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        return ApiResponse.<IntrospectResponse>builder()
                .data(authenticationService.introspect(request))
                .build();
    }
}
