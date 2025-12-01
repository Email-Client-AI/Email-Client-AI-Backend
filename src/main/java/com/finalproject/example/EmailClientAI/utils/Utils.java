package com.finalproject.example.EmailClientAI.utils;

import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Slf4j
public final class Utils {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateRawToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        // "withoutPadding" removes the '=' at the end to make it cleaner
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashToken(String rawToken, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Throw a clear error if the environment variable has a typo
            throw new RuntimeException("Hashing algorithm not supported: " + algorithm, e);
        }
    }
}
