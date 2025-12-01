package com.finalproject.example.EmailClientAI.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.dto.GoogleOAuthDTO;
import com.finalproject.example.EmailClientAI.dto.request.GoogleLoginRequest;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.entity.UserSession;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.mapper.UserMapper;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import com.finalproject.example.EmailClientAI.repository.UserSessionRepository;
import com.finalproject.example.EmailClientAI.service.GoogleOAuthService;
import com.finalproject.example.EmailClientAI.service.JWTService;
import com.finalproject.example.EmailClientAI.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @NonFinal
    @Value("${jwt.accessSignerKey}")
    String accessSignerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    Long validDuration;

    @NonFinal
    @Value("${authentication.hashAlgorithm}")
    String hashAlgorithm;


    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final UserSessionRepository userSessionRepository;
    private final UserMapper userMapper;
    private final JWTService jwtService;

    @Override
    public AuthenticationDTO exchangeCodeAndLogin(GoogleLoginRequest request) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var map = new LinkedMultiValueMap<String, String>();
        map.add("code", request.getCode());
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", redirectUri);
        map.add("grant_type", "authorization_code");

        var entity = new HttpEntity<>(map, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        var body = response.getBody();
        if (body == null) throw new AppException(ErrorCode.UNAUTHENTICATED);

        var googleAccessToken = (String) body.get("access_token");
        var googleRefreshToken = (String) body.get("refresh_token"); // may be null
        var googleIdToken = (String) body.get("id_token");
        var expiresIn = Long.valueOf(body.get("expires_in").toString());
        var refreshTokenExpiresIn =  Long.valueOf(body.get("refresh_token_expires_in").toString());

        // 4. Decode ID Token to get User Info
        var userInfo = extractUserInfo(googleIdToken);
        var user = processUserLogin(userInfo);

        String acId = UUID.randomUUID().toString();
        var appAccessToken = jwtService.generateAccessToken(user, expiresIn, acId);
        String rawToken = Utils.generateRawToken();
        var newUserSession = generateUserSessionWithTokens(user, googleAccessToken, googleRefreshToken, rawToken, refreshTokenExpiresIn);

        userSessionRepository.save(newUserSession);

        var userDTO = userMapper.toDto(user);
        return AuthenticationDTO.builder()
                .refreshToken(rawToken)
                .deviceId(newUserSession.getDeviceId())
                .accessToken(appAccessToken)
                .user(userDTO)
                .build();
    }

    @Override
    public GoogleOAuthDTO refreshAccessToken(String refreshToken) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);
        map.add("grant_type", "refresh_token");

        var entity = new HttpEntity<>(map, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        var googleOAuthDTO = new GoogleOAuthDTO();
        googleOAuthDTO.setAccessToken((String) body.get("access_token"));
        googleOAuthDTO.setAccessTokenExpiresIn(Long.valueOf(body.get("expires_in").toString()));
        googleOAuthDTO.setRefreshTokenExpiresIn(Long.valueOf(body.get("refresh_token_expires_in").toString()));

        return googleOAuthDTO;
    }

    private GoogleOAuthDTO extractUserInfo(String idToken) {
        try {
            // JWT is: Header.Payload.Signature
            String[] parts = idToken.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            JsonNode jsonNode = objectMapper.readTree(payloadJson);

            return GoogleOAuthDTO.builder()
                    .userInfo(new GoogleOAuthDTO.GoogleUserInfo(
                            jsonNode.get("sub").asText(),
                            jsonNode.get("email").asText(),
                            jsonNode.has("name") ? jsonNode.get("name").asText() : "",
                            jsonNode.has("picture") ? jsonNode.get("picture").asText() : ""
                    ))
                    .build();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to decode Google ID Token", e);
        }
    }

    /**
     * Logic to Sync User with Database
     */
    private User processUserLogin(GoogleOAuthDTO googleUser) {
        var googleUserInfo = googleUser.getUserInfo();
        return userRepository.findBySub(googleUserInfo.sub())
                .map(existingUser -> {
                    existingUser.setName(googleUserInfo.name());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    var newUser = User.builder()
                            .sub(googleUserInfo.sub())
                            .email(googleUserInfo.email())
                            .name(googleUserInfo.name())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @Override
    public UserSession generateUserSessionWithTokens(User user, String googleAccessToken, String googleRefreshToken, String appRefreshToken, Long refreshTokenDuration) {

        String appHashedRefreshToken = Utils.hashToken(appRefreshToken, hashAlgorithm);

        String deviceId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(refreshTokenDuration, ChronoUnit.SECONDS);

        return UserSession.builder()
                .userId(user.getId())
                .googleAccessToken(googleAccessToken)
                .googleRefreshToken(googleRefreshToken)
                .appRefreshToken(appHashedRefreshToken)
                .deviceId(deviceId)
                .expiresAt(expiresAt)
                .build();
    }


    // Simple Record to hold extracted data
    private record GoogleUserInfo(String sub, String email, String name, String picture) {}
}
