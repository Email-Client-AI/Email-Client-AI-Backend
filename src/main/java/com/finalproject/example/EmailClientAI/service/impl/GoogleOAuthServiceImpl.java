package com.finalproject.example.EmailClientAI.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.example.EmailClientAI.dto.AuthenticationDTO;
import com.finalproject.example.EmailClientAI.dto.request.GoogleLoginRequest;
import com.finalproject.example.EmailClientAI.dto.response.AuthenticationResponse;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import com.finalproject.example.EmailClientAI.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

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

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;


    @Override
    public AuthenticationDTO exchangeCodeAndLogin(GoogleLoginRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("code", request.getCode());
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", redirectUri);
        map.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        Map<String, Object> body = response.getBody();
        if (body == null) throw new RuntimeException("Empty response from Google");

        String accessToken  = (String) body.get("access_token");
        String refreshToken = (String) body.get("refresh_token"); // may be null
        String idTokenString = (String) body.get("id_token");
        Long expiresIn = Long.valueOf(body.get("expires_in").toString());

        // 4. Decode ID Token to get User Info
        GoogleUserInfo userInfo = extractUserInfo(idTokenString);

        // 5. Find or Create User in DB
        User user = processUserLogin(userInfo, refreshToken, accessToken, expiresIn);

        // Step 3: Find or create user in your DB
        User user = userRepository.findBySub()

        // Step 4: Store refresh token securely (if received)
        if (refreshToken != null) {
            user.setGoogleRefreshToken(refreshToken); // encrypt in real app!
            userRepository.save(user);
        }

        // Step 5: Generate your own JWT for session
        String yourJwt = jwtTokenProvider.generateToken(user);

        // Step 6: Return everything to frontend
        return new GoogleAuthResponse(
                accessToken,
                refreshToken,
                idTokenString,
                expiresIn,
                (String) body.get("scope"),
                yourJwt
        );
    }

    private GoogleUserInfo extractUserInfo(String idToken) {
        try {
            // JWT is: Header.Payload.Signature
            String[] parts = idToken.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            JsonNode jsonNode = objectMapper.readTree(payloadJson);

            return new GoogleUserInfo(
                    jsonNode.get("sub").asText(),
                    jsonNode.get("email").asText(),
                    jsonNode.has("name") ? jsonNode.get("name").asText() : "",
                    jsonNode.has("picture") ? jsonNode.get("picture").asText() : ""
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to decode Google ID Token", e);
        }
    }

    /**
     * Logic to Sync User with Database
     */
    private User processUserLogin(GoogleUserInfo googleUser, String googleRefreshToken, String google) {
        // Try to find by Google ID ("sub")
        return userRepository.findBySub(googleUser.sub())
                .map(existingUser -> {
                    // Update existing user info if needed
                    existingUser.setName(googleUser.name());
                    // If we got a new refresh token from Google, save it (encrypt this in real life!)
                    if (googleRefreshToken != null) {
                        existingUser.setGoogleRefreshToken(googleRefreshToken);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // Create new user
                    // Check if email already exists (e.g., signed up with password previously)
                    return userRepository.findByEmail(googleUser.email())
                            .map(existingEmailUser -> {
                                // Link Google account to existing email account
                                existingEmailUser.setSub(googleUser.sub());
                                if (googleRefreshToken != null) {
                                    existingEmailUser.setGoogleRefreshToken(googleRefreshToken);
                                }
                                return userRepository.save(existingEmailUser);
                            })
                            .orElseGet(() -> {
                                // Totally new user
                                User newUser = new User();
                                newUser.setEmail(googleUser.email());
                                newUser.setName(googleUser.name());
                                newUser.setSub(googleUser.sub());

                                // Handle Non-Nullable Password
                                // Generate a random UUID as password since they login via Google
                                newUser.setPassword(UUID.randomUUID().toString());

                                if (googleRefreshToken != null) {
                                    newUser.setGoogleRefreshToken(googleRefreshToken);
                                }

                                return userRepository.save(newUser);
                            });
                });
    }

    // Simple Record to hold extracted data
    private record GoogleUserInfo(String sub, String email, String name, String picture) {}
}
