package com.example.tradeservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Custom thread pool for trading strategy execution
     */
    @Bean(name = "tradingTaskExecutor")
    public Executor tradingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4); // Number of symbols you typically process
        executor.setMaxPoolSize(10); // Maximum threads
        executor.setQueueCapacity(20); // Queue capacity
        executor.setThreadNamePrefix("Trading-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
