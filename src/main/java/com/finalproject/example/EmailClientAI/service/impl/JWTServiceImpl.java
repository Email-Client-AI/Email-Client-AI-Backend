package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.service.JWTService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JWTServiceImpl implements JWTService {

    @NonFinal
    @Value("${jwt.accessSignerKey}")
    String accessSignerKey;

    @Override
    public String generateAccessToken(User user, Long duration, String id) {
        if (accessSignerKey == null || accessSignerKey.isEmpty()) {
            log.error("Signer key is null or empty!");
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (accessSignerKey.length() < 32) {
            log.error("Signer key is too short: {} bytes (minimum 32 required)", accessSignerKey.length());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512).build();
        JWTClaimsSet claimsSet;

        claimsSet = buildAccessTokenClaims(user, duration);

        Payload payload = claimsSet.toPayload();

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            log.debug("Signing token with MACSigner...");
            jwsObject.sign(new MACSigner(accessSignerKey));
            String token = jwsObject.serialize();
            log.debug("Token generated successfully");
            return token;
        } catch (JOSEException e) {
            log.error("Error signing token: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public JWTClaimsSet buildAccessTokenClaims(User user, Long duration) {
        return new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getId()))
                .issuer("EmailClientAI")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .build();
    }


    @Override
    public boolean isTokenValid(String token) {
        return false;
    }

    @Override
    public String extractClaim(String token, String claimKey) {
        return "";
    }

    @Override
    public JWTClaimsSet parseToken(String token) {
        return null;
    }

    @Override
    public String extractSubject(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime).before(new Date());
    }


    @Override
    public boolean isTokenValid(String token, User user) {
        final String id = extractSubject(token);
        return (id.equals(user.getId().toString()) && !isTokenExpired(token));
    }

    @Override
    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        try {
            // Parse the token
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Get the body (ClaimsSet)
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Apply the function to extract the specific data
            return claimsResolver.apply(claims);

        } catch (ParseException e) {
            log.error("Failed to parse JWT token: ", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
