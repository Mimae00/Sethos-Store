package com.possystem.pos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The Angular app is served from its own origin, so the API has to opt into CORS.
 * Allowed origins come from configuration; no wildcard, so the policy stays usable once
 * cookies or auth headers are added later.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PosProperties properties;

    public WebConfig(PosProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(properties.allowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
