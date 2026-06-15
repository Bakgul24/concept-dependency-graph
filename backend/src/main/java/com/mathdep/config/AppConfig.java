package com.mathdep.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig {

    /**
     * ChatClient.Builder is auto-configured by Spring AI.
     * We expose it here so it can be injected into services.
     * Custom defaults (retry, advisors) can be added here.
     */
    @Bean("customChatClientBuilder")
    public ChatClient.Builder chatClientBuilder(ChatClient.Builder builder) {
        return builder;
    }

    /**
     * CORS configuration for React dev server.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "http://localhost:5173",  // Vite dev server
                        "http://localhost:3000"   // CRA dev server
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .maxAge(3600);
            }
        };
    }
}
