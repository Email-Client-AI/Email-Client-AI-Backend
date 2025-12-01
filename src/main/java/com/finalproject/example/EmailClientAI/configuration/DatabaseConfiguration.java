package com.finalproject.example.EmailClientAI.configuration;

import com.finalproject.example.EmailClientAI.security.SpringSecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class DatabaseConfiguration {
    @Bean
    AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }
}
