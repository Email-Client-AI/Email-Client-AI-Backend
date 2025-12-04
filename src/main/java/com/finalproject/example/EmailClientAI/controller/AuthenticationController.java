package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.dto.request.*;
import com.finalproject.example.EmailClientAI.dto.response.ApiResponse;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.dto.response.IntrospectResponse;
import com.finalproject.example.EmailClientAI.service.AuthenticationService;
import com.finalproject.example.EmailClientAI.service.GoogleOAuthService;
import com.finalproject.example.EmailClientAI.utils.Utils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/google")
    public ResponseEntity<AuthenticationDTO> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        var authenticationDTO = googleOAuthService.exchangeCodeAndLogin(request);
        var refreshCookie = authenticationService.createHttpOnlyCookie("refresh_token", authenticationDTO.getRefreshToken());
        var deviceCookie = authenticationService.createHttpOnlyCookie("device_id", authenticationDTO.getDeviceId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deviceCookie.toString())
                .body(authenticationDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationDTO> refresh(@CookieValue(name = "refresh_token") String refreshToken,
                                                  @CookieValue(name = "device_id") String deviceId) {
        var authenticationDTO = authenticationService.refresh(refreshToken, deviceId);
        var refreshCookie = authenticationService.createHttpOnlyCookie("refresh_token", authenticationDTO.getRefreshToken());
        var deviceCookie = authenticationService.createHttpOnlyCookie("device_id", authenticationDTO.getDeviceId());

        log.info("Refreshed tokens for device ID: {}, the new device ID: {}", deviceId, authenticationDTO.getDeviceId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deviceCookie.toString())
                .body(authenticationDTO);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token") String refreshToken,
                                       @CookieValue(name = "device_id") String deviceId) {
        authenticationService.logout(refreshToken, deviceId);
        return ResponseEntity.noContent().build();
    }

}
