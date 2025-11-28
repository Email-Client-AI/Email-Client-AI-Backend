package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.configuration.enumeration.AuthenticationTokenType;
import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.repository.UserSessionRepository;
import com.finalproject.example.EmailClientAI.service.AuthenticationService;
import com.finalproject.example.EmailClientAI.utils.Utils;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.finalproject.example.EmailClientAI.dto.request.*;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.dto.response.IntrospectResponse;
import com.finalproject.example.EmailClientAI.dto.UserDTO;
import com.finalproject.example.EmailClientAI.entity.InvalidatedToken;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.InvalidatedTokenRepository;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceIml implements AuthenticationService {

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean isValid = true;

        try {
            verifyToken(request.getToken(), false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Register request received: {}", request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        log.info("Saving user to database...");
        user = userRepository.save(user);
        log.info("User saved successfully with ID: {}", user.getId());

        try {
            log.info("Building authentication response...");
            AuthenticationResponse response = buildAuthenticationResponse(user);
            log.info("Authentication response built successfully");
            return response;
        } catch (Exception e) {
            log.error("Error building authentication response: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_EMAIL_PASSWORD));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_PASSWORD);
        }

        return buildAuthenticationResponse(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String deviceId) {
        if(StringUtils.isBlank(refreshToken) || StringUtils.isBlank(deviceId)) {
            throw new AppException(ErrorCode.INVALID_LOGOUT_REQUEST);
        }

        var userSessionOpt = userSessionRepository.findByAppRefreshTokenAndDeviceId(
                Utils.hashToken(refreshToken, hashAlgorithm), deviceId);

        if (userSessionOpt.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_LOGOUT_REQUEST);
        }
        userSessionRepository.delete(userSessionOpt.get());
    }

    @Override
    @Transactional
    public AuthenticationDTO refresh(String refreshToken, String deviceId) {
        if(StringUtils.isBlank(refreshToken) || StringUtils.isBlank(deviceId)) {
            throw new AppException(ErrorCode.INVALID_LOGOUT_REQUEST);
        }

        var userSessionOpt = userSessionRepository.findByAppRefreshTokenAndDeviceId(
                Utils.hashToken(refreshToken, hashAlgorithm), deviceId);

        if (userSessionOpt.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_LOGOUT_REQUEST);
        }
        userSessionRepository.delete(userSessionOpt.get());
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

    private SignedJWT verifyToken(String token, boolean isRefresh) {
        JWSVerifier verifier;

        try {
            if (isRefresh) {
                verifier = new MACVerifier(REFRESH_SIGNER_KEY);
            } else {
                verifier = new MACVerifier(ACCESS_SIGNER_KEY);
            }

            SignedJWT signedJWT = SignedJWT.parse(token);

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            String id = signedJWT.getJWTClaimsSet().getJWTID();
            boolean verified = signedJWT.verify(verifier);

            if (!verified || expirationTime.before(new Date())) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            if (invalidatedTokenRepository.existsByAccessIdOrRefreshId(id)) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            return signedJWT;
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    private boolean validateRefreshTokenWithDevice(String refreshToken, String deviceId) {
        var userSessionOpt = userSessionRepository.findByAppRefreshTokenAndDeviceId(
                Utils.hashToken(refreshToken, hashAlgorithm), deviceId);
        if (userSessionOpt.isEmpty()) {
            return false;
        } else {
            var userSession = userSessionOpt.get();
            if (userSession.getExpiresAt().isBefore(Instant.now())) {
                return false;
            }
            return true;
        }
    }


}
