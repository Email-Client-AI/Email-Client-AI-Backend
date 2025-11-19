package com.finalproject.example.EmailClientAI.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment Configuration Initializer
 * 
 * Loads environment variables from .env file before Spring Boot application
 * context starts.
 * This allows Spring to use these variables in application.yaml with
 * ${VAR_NAME} syntax.
 */
public class EnvironmentConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    try {
      // Load .env file from project root
      Dotenv dotenv = Dotenv.configure()
          .ignoreIfMissing() // Don't fail if .env is missing (use defaults)
          .load();

      // Create property source from .env variables
      Map<String, Object> envMap = new HashMap<>();
      dotenv.entries().forEach(entry -> {
        envMap.put(entry.getKey(), entry.getValue());
      });

      // Add to Spring environment with high priority
      ConfigurableEnvironment environment = applicationContext.getEnvironment();
      environment.getPropertySources()
          .addFirst(new MapPropertySource("dotenvProperties", envMap));

      System.out.println("Environment variables loaded from .env file successfully!");

    } catch (Exception e) {
      System.err.println("Warning: Could not load .env file. Using default values from application.yaml");
      // Don't fail - let Spring use default values
    }
  }
}
