package com.possystem.pos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Cross-cutting beans.
 */
@Configuration
@EnableJpaAuditing
@EnableConfigurationProperties(PosProperties.class)
public class ApplicationConfig {

    /**
     * Time is injected rather than read from {@code Instant.now()} at call sites, so
     * "today" is defined by the store's configured zone and can be frozen in tests.
     */
    @Bean
    public Clock clock(PosProperties properties) {
        return Clock.system(ZoneId.of(properties.timeZone()));
    }
}
