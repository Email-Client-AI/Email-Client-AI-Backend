package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.entity.UserSession;
import com.finalproject.example.EmailClientAI.repository.UserSessionRepository;
import com.finalproject.example.EmailClientAI.service.AuthenticationService;
import com.finalproject.example.EmailClientAI.service.GoogleOAuthService;
import com.finalproject.example.EmailClientAI.service.JWTService;
import com.finalproject.example.EmailClientAI.utils.Utils;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.finalproject.example.EmailClientAI.dto.request.*;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.dto.response.IntrospectResponse;
import com.finalproject.example.EmailClientAI.dto.UserDTO;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceIml implements AuthenticationService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleOAuthService googleOAuthService;
    private final JWTService jwtService;

    @NonFinal
    @Value("${jwt.accessSignerKey}")
    String ACCESS_SIGNER_KEY;

    @NonFinal
    @Value("${jwt.refreshSignerKey}")
    String REFRESH_SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${google.client-id}")
    String GOOGLE_CLIENT_ID;

    @NonFinal
    @Value("${authentication.hashAlgorithm}")
    String hashAlgorithm;

    @NonFinal
    @Value("${environment}")
    String environment;



    @Override
    @Transactional
    public void logout(String refreshToken, String deviceId) {
        if(StringUtils.isBlank(refreshToken) || StringUtils.isBlank(deviceId)) {
            throw new AppException(ErrorCode.INVALID_LOGOUT_REQUEST);
        }

        var userSession = userSessionRepository.findByAppRefreshTokenAndDeviceId(
                        Utils.hashToken(refreshToken, hashAlgorithm), deviceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_REQUEST));

        if (userSession.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }
        userSessionRepository.delete(userSession);
    }

    @Override
    @Transactional
    public AuthenticationDTO refresh(String refreshToken, String deviceId) {
        if(StringUtils.isBlank(refreshToken) || StringUtils.isBlank(deviceId)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_REQUEST);
        }
        var hashedRefreshToken = Utils.hashToken(refreshToken, hashAlgorithm);
        var userSession = userSessionRepository.findByAppRefreshTokenAndDeviceId(
                        hashedRefreshToken, deviceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_REQUEST));

        if (userSession.getExpiresAt().isBefore(Instant.now())) {
            userSessionRepository.delete(userSession);
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }

        var googleOAuthDTO = googleOAuthService.refreshAccessToken(userSession.getGoogleRefreshToken());
        String acId = UUID.randomUUID().toString();
        var appAccessToken = jwtService.generateAccessToken(userSession.getUser(), googleOAuthDTO.getAccessTokenExpiresIn(), acId);
        String rawToken = Utils.generateRawToken();
        var updatedUserSession = googleOAuthService.generateUserSessionWithTokens(userSession.getUser(), googleOAuthDTO.getAccessToken(), userSession.getGoogleAccessToken(), rawToken, googleOAuthDTO.getRefreshTokenExpiresIn());
        updatedUserSession.setId(userSession.getId());

        userSessionRepository.save(updatedUserSession);

        return AuthenticationDTO.builder()
                .refreshToken(rawToken)
                .deviceId(updatedUserSession.getDeviceId())
                .accessToken(appAccessToken)
                .build();
    }

    @Override
    public ResponseCookie createHttpOnlyCookie(String name, String value) {
        String env = environment.toLowerCase();
        return ResponseCookie.from(name, value)
                .httpOnly(true)       // 1. JavaScript cannot read this
                .secure(env.equals("production"))
                .maxAge(7 * 24 * 60 * 60)
                .sameSite(env.equals("production") ? "None" : "Lax")   // 3. CSRF protection
                .path("/")            // 4. Available across the app
                .build();
    }




    private AuthenticationResponse buildAuthenticationResponse(User user) {
        String acId = UUID.randomUUID().toString();
        String rfId = UUID.randomUUID().toString();

        // String accessToken = Utils.generateToken(user, VALID_DURATION, acId, rfId, ACCESS_SIGNER_KEY, AuthenticationTokenType.ACCESS_TOKEN);
        // String refreshToken = Utils.generateToken(user, REFRESHABLE_DURATION, rfId, acId, REFRESH_SIGNER_KEY, AuthenticationTokenType.REFRESH_TOKEN);

        UserDTO userDTO = UserDTO.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .name(user.getName())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(null)
                .user(userDTO)
                .build();
    }

}
