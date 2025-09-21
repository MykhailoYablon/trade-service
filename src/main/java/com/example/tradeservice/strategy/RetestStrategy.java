package com.example.tradeservice.strategy;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.strategy.enums.TradingState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static com.example.tradeservice.strategy.utils.FileUtils.writeToLog;

@Slf4j
@Service
@AllArgsConstructor
public class RetestStrategy {

    private final AsyncOpeningRangeBreakoutStrategy orbService;
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
//                .thenCompose(isBreak -> {
//                    if(isBreak) {
//                        checkProfit(symbol, date);
//                    }
//                    return new CompletableFuture<Void>();
//                })
                .exceptionally(e -> {
                    log.error("[{}] Error monitoring for breakout/retest", symbol, e);
                    return null;
                });
    }

    private CompletableFuture<Void> collectOpeningRangeDataSequentially(String symbol, String date, int count) {
        if (count <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        return orbService.collectOpeningRangeDataAsync(symbol, date)
                .thenCompose(v -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .thenCompose(v -> collectOpeningRangeDataSequentially(symbol, date, count - 1));
    }

    private CompletableFuture<Void> monitorSymbolLoop(String symbol, String date, int maxIterations) {

        if (orbService.isBreak(symbol + date)) {
            return CompletableFuture.completedFuture(null);
        }
        if (maxIterations <= 0 || !orbService.shouldMonitorSymbol(symbol + date)) {
            return CompletableFuture.completedFuture(null);
        }

        return orbService.monitorSymbolAsync(symbol, date)
                .thenCompose(v -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .thenCompose(v -> monitorSymbolLoop(symbol, date, maxIterations - 1));
    }

    private CompletableFuture<Void> checkProfit(String symbol, String date) {
        log.info("Checking profit");
        return this.checkMinute(symbol, date)
                .thenCompose(v -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .thenCompose(v -> checkProfit(symbol, date));
    }

    private CompletableFuture<Void> checkMinute(String symbol, String date) {
        TwelveCandleBar oneMinBar;
        try {
            oneMinBar = dataClient.fetchNextCandle(symbol, TimeFrame.ONE_MIN, date);
            if (oneMinBar != null) {
//                writeToLog(symbol + "/" + state.getTestDate() + ".log",
//                        String.format("[%s] One min bar added: Close=%s > Opening High=%s",
//                                symbol, oneMinBar.getClose(), state.getOpeningRange().high()));
            }
        } catch (Exception e) {
            log.error("[{}] Error monitoring for breakout/retest", symbol, e);
        }

        return CompletableFuture.completedFuture(null);
    }
}
