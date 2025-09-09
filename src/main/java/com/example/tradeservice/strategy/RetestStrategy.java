package com.example.tradeservice.strategy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@AllArgsConstructor
public class RetestStrategy {

    private final AsyncOpeningRangeBreakoutService orbService;
    private final CsvStockDataClient dataClient;

    @Async
    public CompletableFuture<Void> startStrategy(String symbol, String date) {
        dataClient.initializeCsvForDay(symbol, date);
        try {
            //collect three 5 min ranges
            for (int i = 0; i < 3; i++) {
                orbService.collectOpeningRangeDataAsync(symbol, date);
                Thread.sleep(1000);
            }

            //
            for (int i = 0; i < 100; i++) {
                if (orbService.shouldMonitorSymbol(symbol + date)) {
                    orbService.monitorSymbolAsync(symbol, date);
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            log.error("[{}] Error monitoring for breakout/retest", symbol, e);
        }

        return CompletableFuture.completedFuture(null);
    }
}
