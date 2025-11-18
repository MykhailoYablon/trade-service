package com.strategy.trade.configuration.interceptor;

import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RateLimitInterceptor implements ClientHttpRequestInterceptor {
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final int maxRequests;
    private final Duration period;

    public RateLimitInterceptor(int maxRequests, Duration period) {
        this.maxRequests = maxRequests;
        this.period = period;
        this.semaphore = new Semaphore(maxRequests);
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Schedule permit release
        long periodMillis = period.toMillis();
        long delayPerRequest = periodMillis / maxRequests;

        scheduler.scheduleAtFixedRate(
                () -> {
                    if (semaphore.availablePermits() < maxRequests) {
                        semaphore.release();
                    }
                },
                delayPerRequest,
                delayPerRequest,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public org.springframework.http.client.ClientHttpResponse intercept(
            org.springframework.http.HttpRequest request,
            byte[] body,
            org.springframework.http.client.ClientHttpRequestExecution execution) throws IOException {

        try {
            // Wait for available permit (blocks if rate limit reached)
            semaphore.acquire();

            // Execute the request
            return execution.execute(request, body);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Rate limit interrupt", e);
        }
    }
}
