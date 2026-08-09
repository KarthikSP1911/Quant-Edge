package com.quantedge.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4's JacksonAutoConfiguration wires a {@code tools.jackson.databind.ObjectMapper}
 * (Jackson 3), not the classic {@code com.fasterxml.jackson.databind.ObjectMapper} this codebase
 * uses elsewhere (jjwt, Kafka serializers) - so nothing auto-registers the classic bean that
 * ChatService needs. jackson-databind and jackson-datatype-jsr310 are already compile
 * dependencies (see pom.xml) for that reason; this just exposes them as a bean.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
