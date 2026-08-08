package com.quantedge.backend.external;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalApiClientConfig {

    @Bean
    public RestClient finnhubRestClient(
            @Value("${finnhub.base-url}") String baseUrl, @Value("${finnhub.timeout-ms}") long timeoutMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(timeoutMs))
                .build();
    }

    @Bean
    public RestClient twelveDataRestClient(
            @Value("${twelvedata.base-url}") String baseUrl, @Value("${twelvedata.timeout-ms}") long timeoutMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(timeoutMs))
                .build();
    }

    @Bean
    public RestClient alphaVantageRestClient(
            @Value("${alphavantage.base-url}") String baseUrl, @Value("${alphavantage.timeout-ms}") long timeoutMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(timeoutMs))
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return factory;
    }
}
