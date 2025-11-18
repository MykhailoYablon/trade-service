package com.strategy.trade.configuration;

import com.strategy.trade.configuration.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    @Qualifier("twelveClient")
    public RestClient twelveClient(@Qualifier("rateLimitInterceptor") ClientHttpRequestInterceptor interceptor) {
        return RestClient.builder()
                .baseUrl("https://api.twelvedata.com") // Your API base URL
                .requestInterceptor(interceptor)
                .build();
    }

    @Bean
    @Qualifier("rateLimitInterceptor")
    public ClientHttpRequestInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(4, Duration.ofMinutes(1));
    }

}
