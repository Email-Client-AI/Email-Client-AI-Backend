package com.finalproject.example.EmailClientAI.security;

import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.entity.UserSession;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;

@UtilityClass
public final class SecurityUtils {
    public static Optional<String> getCurrentLoggedInUserName() {
        var securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractUserName(securityContext.getAuthentication()));
    }

    public static Optional<String> getCurrentLoggedInUserId() {
        var securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractUserId(securityContext.getAuthentication()));
    }

    public static Optional<User> getCurrentLoggedInUser() {
        var securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }


    public static boolean isCurrentUserMatchingSession(UserSession userSession) {
        var currLoggedInUser = SecurityUtils.getCurrentLoggedInUser();
        return currLoggedInUser.isPresent() && currLoggedInUser.get().getId().equals(userSession.getUserId());
    }

    private static String extractUserName(Authentication authentication) {
        if (Objects.nonNull(authentication) && authentication.getPrincipal() instanceof User user) {
            return user.getName();
        }
        return "admin";
    }

    private static User extractPrincipal(Authentication authentication) {
        if (Objects.nonNull(authentication) && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private static String extractUserId(Authentication authentication) {
        if (Objects.nonNull(authentication) && authentication.getPrincipal() instanceof User user) {
            return user.getId().toString();
        }
        return "admin";
    }

}
