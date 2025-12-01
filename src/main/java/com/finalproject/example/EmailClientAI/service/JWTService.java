package com.finalproject.example.EmailClientAI.service;


import com.finalproject.example.EmailClientAI.entity.User;
import com.nimbusds.jwt.JWTClaimsSet;

import java.util.function.Function;

public interface JWTService {

    String generateAccessToken(User user, Long duration, String id);

    JWTClaimsSet buildAccessTokenClaims(User user, Long duration);

    boolean isTokenValid(String token);

    String extractClaim(String token, String claimKey);

    JWTClaimsSet parseToken(String token);

    String extractSubject(String token);

    <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver);

    boolean isTokenExpired(String token);

    boolean isTokenValid(String token, User user);
}
