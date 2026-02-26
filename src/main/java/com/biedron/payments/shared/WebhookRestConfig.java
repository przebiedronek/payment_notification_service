package com.biedron.payments.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class WebhookRestConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(@Value("${spring.application.app.rest.timeout}") int timeout) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(timeout));
        return factory;
    }

    @Bean
    public RestClient restClient(
            RestClient.Builder builder,
            ClientHttpRequestFactory clientHttpRequestFactory,
            @Value("${spring.application.app.webhook.api-key}") String apiKey) {
        return builder
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }
}

