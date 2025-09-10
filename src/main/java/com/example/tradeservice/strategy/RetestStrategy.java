package com.example.tradeservice.strategy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@AllArgsConstructor
public class RetestStrategy {

    private final AsyncOpeningRangeBreakoutService orbService;
    private final CsvStockDataClient dataClient;

    @Async("strategyExecutor")
    public CompletableFuture<Void> startStrategy(String symbol, String date) {
        new File("logs/" + symbol + "/break").mkdirs();
        return CompletableFuture
                .runAsync(() -> {
                    dataClient.initializeCsvForDay(symbol, date);
                    log.info("Completed CSV initialization for {} on {}", symbol, date);
                })
                .thenCompose(v ->{
                    log.info("Moving to collect opening range data for {} on {}", symbol, date);
                    return collectOpeningRangeDataSequentially(symbol, date, 3);
                })
                .thenCompose(v -> monitorSymbolLoop(symbol, date, 100))
                .exceptionally(e -> {
                    log.error("[{}] Error monitoring for breakout/retest", symbol, e);
                    return null;
                });
    }

    private CompletableFuture<Void> collectOpeningRangeDataSequentially(String symbol, String date, int count) {
        if (count <= 0) {
            log.info("Test");
            return CompletableFuture.completedFuture(null);
        }
        log.info("collectOpeningRangeDataAsync");
        return orbService.collectOpeningRangeDataAsync(symbol, date)
//                .thenCompose(v -> CompletableFuture.runAsync(() -> {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }))
                .thenCompose(v -> collectOpeningRangeDataSequentially(symbol, date, count - 1));
    }

    private CompletableFuture<Void> monitorSymbolLoop(String symbol, String date, int maxIterations) {
        if (maxIterations <= 0 || !orbService.shouldMonitorSymbol(symbol + date)) {
            return CompletableFuture.completedFuture(null);
        }

        return orbService.monitorSymbolAsync(symbol, date)
//                .thenCompose(v -> CompletableFuture.runAsync(() -> {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }))
                .thenCompose(v -> monitorSymbolLoop(symbol, date, maxIterations - 1));
    }
}
