package com.finalproject.example.EmailClientAI.service.impl;

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

        // Step 3: Find or create user in your DB
        User user = userRepository.findByGoogleId(googl
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> {
                            existing.setGoogleId(googleId);
                            return userRepository.save(existing);
                        })
                        .orElseGet(() -> {
                            User newUser = User.builder()
                                    .googleId(googleId)
                                    .email(email)
                                    .name(name)
                                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                    .build();
                            return userRepository.save(newUser);
                        }));

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
}
