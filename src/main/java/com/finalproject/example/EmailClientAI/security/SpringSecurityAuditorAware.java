package com.finalproject.example.EmailClientAI.security;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class SpringSecurityAuditorAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        return SecurityUtils.getCurrentLoggedInUserId();
    }
}