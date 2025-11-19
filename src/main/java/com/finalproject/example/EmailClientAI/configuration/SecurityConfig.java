package com.finalproject.example.EmailClientAI.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableMethodSecurity
public class SecurityConfig {

  @NonFinal
  String[] PUBLIC_POST_ENDPOINTS = {
      "/auth/register",
      "/auth/login",
      "/auth/google",
      "/auth/refresh",
      "/auth/logout",
      "/auth/introspect"
  };

  @NonFinal
  String[] PUBLIC_GET_ENDPOINTS = {
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomJwtDecoder decoder) throws Exception {

    http.authorizeHttpRequests(configurer -> {
      configurer
          .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
          .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
          .anyRequest().authenticated();
    });

    http.csrf(AbstractHttpConfigurer::disable);

    // Disable form login and HTTP Basic authentication
    http.formLogin(AbstractHttpConfigurer::disable);
    http.httpBasic(AbstractHttpConfigurer::disable);

    http.oauth2ResourceServer(configurer -> configurer.jwt(jwtConfigurer -> jwtConfigurer
        .decoder(decoder)
        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

    return http.build();
  }

  @Bean
  public CorsFilter corsFilter(@Value("${client.url}") String clientUrl) {
    log.info("Client URL: {}", clientUrl);

    CorsConfiguration config = new CorsConfiguration();

    config.addAllowedOrigin(clientUrl);
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
    urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", config);

    return new CorsFilter(urlBasedCorsConfigurationSource);
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
    converter.setAuthorityPrefix("");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);

    return jwtAuthenticationConverter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
