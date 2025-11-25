package com.finalproject.example.EmailClientAI.utils;

import com.finalproject.example.EmailClientAI.configuration.enumeration.AuthenticationTokenType;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
public final class Utils {

    public static String generateToken(User user, long duration, String id, String otherId, String signerKey, AuthenticationTokenType tokenType) {
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

        if (tokenType == AuthenticationTokenType.ACCESS_TOKEN) {
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

    public static JWTClaimsSet buildAccessTokenClaims(User user, long duration, String id, String otherId) {
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

    public static JWTClaimsSet buildRefreshTokenClaims(User user, long duration, String id, String otherId) {
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
