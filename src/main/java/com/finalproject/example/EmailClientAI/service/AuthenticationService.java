package com.finalproject.example.EmailClientAI.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.finalproject.example.EmailClientAI.dto.request.*;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.dto.response.IntrospectResponse;
import com.finalproject.example.EmailClientAI.dto.response.UserResponse;
import com.finalproject.example.EmailClientAI.entity.InvalidatedToken;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.InvalidatedTokenRepository;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    InvalidatedTokenRepository invalidatedTokenRepository;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

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

    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_EMAIL_PASSWORD));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_PASSWORD);
        }

        return buildAuthenticationResponse(user);
    }

    @Transactional
    public AuthenticationResponse loginWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getGoogleToken());
            if (idToken == null) {
                throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Check if user exists with Google ID
            User user = userRepository.findByGoogleId(googleId)
                    .orElseGet(() -> {
                        // Check if user exists with email
                        return userRepository.findByEmail(email)
                                .map(existingUser -> {
                                    // Link Google account to existing user
                                    existingUser.setGoogleId(googleId);
                                    return userRepository.save(existingUser);
                                })
                                .orElseGet(() -> {
                                    // Create new user
                                    User newUser = User.builder()
                                            .email(email)
                                            .name(name)
                                            .googleId(googleId)
                                            .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                                            .build();
                                    return userRepository.save(newUser);
                                });
                    });

            return buildAuthenticationResponse(user);

        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }

    @Transactional
    public void logout(LogoutRequest request) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(request.getToken());
            String acId = signedJWT.getJWTClaimsSet().getJWTID();
            String rfId = signedJWT.getJWTClaimsSet().getClaim("rfId").toString();

            Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
            LocalDateTime expirationTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());
            expirationTime = expirationTime.plusSeconds(REFRESHABLE_DURATION - VALID_DURATION);

            invalidatedTokenRepository.save(InvalidatedToken.builder()
                    .accessId(acId)
                    .refreshId(rfId)
                    .expirationTime(expirationTime)
                    .build());
        } catch (ParseException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    public AuthenticationResponse refresh(RefreshTokenRequest request) {
        SignedJWT signedJWT = verifyToken(request.getRefreshToken(), true);

        try {
            String acId = signedJWT.getJWTClaimsSet().getClaim("acId").toString();
            String rfId = signedJWT.getJWTClaimsSet().getJWTID();

            Instant expirationInstant = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
            LocalDateTime expirationTime = LocalDateTime.ofInstant(expirationInstant, ZoneId.systemDefault());

            invalidatedTokenRepository.save(InvalidatedToken.builder()
                    .accessId(acId)
                    .refreshId(rfId)
                    .expirationTime(expirationTime)
                    .build());

            User user = userRepository.findById(signedJWT.getJWTClaimsSet().getSubject())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            return buildAuthenticationResponse(user);
        } catch (ParseException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    private AuthenticationResponse buildAuthenticationResponse(User user) {
        String acId = UUID.randomUUID().toString();
        String rfId = UUID.randomUUID().toString();

        String accessToken = generateToken(user, VALID_DURATION, acId, rfId, ACCESS_SIGNER_KEY);
        String refreshToken = generateToken(user, REFRESHABLE_DURATION, rfId, acId, REFRESH_SIGNER_KEY);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
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

    private String generateToken(User user, long duration, String id, String otherId, String signerKey) {
        log.debug("Generating token - signerKey length: {}, duration: {}",
                signerKey != null ? signerKey.length() : "null", duration);

        if (signerKey == null || signerKey.isEmpty()) {
            log.error("Signer key is null or empty!");
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (signerKey.length() < 32) {
            log.error("Signer key is too short: {} bytes (minimum 32 required)", signerKey.length());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
        JWTClaimsSet claimsSet;

        if (signerKey.equals(ACCESS_SIGNER_KEY)) {
            log.debug("Building access token claims");
            claimsSet = buildAccessTokenClaims(user, duration, id, otherId);
        } else {
            log.debug("Building refresh token claims");
            claimsSet = buildRefreshTokenClaims(user, duration, id, otherId);
        }

        Payload payload = claimsSet.toPayload();

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            log.debug("Signing token with MACSigner...");
            jwsObject.sign(new MACSigner(signerKey));
            String token = jwsObject.serialize();
            log.debug("Token generated successfully");
            return token;
        } catch (JOSEException e) {
            log.error("Error signing token: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    private JWTClaimsSet buildAccessTokenClaims(User user, long duration, String id, String otherId) {
        return new JWTClaimsSet.Builder()
                .subject(user.getId())
                .jwtID(id)
                .issuer("EmailClientAI")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
                .claim("rfId", otherId)
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .build();
    }

    private JWTClaimsSet buildRefreshTokenClaims(User user, long duration, String id, String otherId) {
        return new JWTClaimsSet.Builder()
                .subject(user.getId())
                .jwtID(id)
                .issuer("EmailClientAI")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
                .claim("acId", otherId)
                .build();
    }
}
