package com.netflixai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TmdbClientConfiguration {

    @Bean
    RestClient.Builder tmdbRestClientBuilder() {
        return RestClient.builder();
    }
}
