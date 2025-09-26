package com.example.tradeservice.strategy;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.strategy.dataclient.StockDataClient;
import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.tradeservice.strategy.enums.TradingState.MONITORING_FOR_BREAKOUT;
import static com.example.tradeservice.strategy.enums.TradingState.MONITORING_FOR_RETEST;

@Slf4j
@Service
public class RetestAsyncTradingStrategy {

    // Thread-safe state tracking for multiple symbols
    private final Map<String, TradingContext> symbolContexts = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("csvDataStrategy")
    private AsyncTradingStrategy asyncOrbStrategy;

    @Autowired
    @Qualifier("csvData")
    private StockDataClient dataClient;

    @Async("strategyExecutor")
    public CompletableFuture<List<Order>> startStrategy(String symbol, String date) {
        new File("logs/" + symbol + "/break").mkdirs();
        return CompletableFuture
                .runAsync(() -> dataClient.initializeCsvForDay(symbol, date))
                .thenCompose(v -> asyncOrbStrategy.startStrategy(new TradingContext(symbol, date, new SymbolTradingState(),
                        StrategyMode.BACKTEST)))
                .thenCompose(context -> {
                    if (context != null) {
                        symbolContexts.put(context.symbol(), context);
                        log.info("Saved context for symbol: {}", context.symbol());
                    }
                    return onTick(context, 100);
                })
//                .thenCompose(isBreak -> {
//                    if(!isBreak.isEmpty()) {
//                        checkResult(symbol, date);
//                    }
//                    return new CompletableFuture<Void>();
//                })
                .exceptionally(e -> {
                    log.error("[{}] Error monitoring for breakout/retest", symbol, e);
                    return null;
                });
    }

    public CompletableFuture<List<Order>> onTick(TradingContext context, int maxIterations) {

//        if (orbService.isBreak(symbol + date)) {
//            return CompletableFuture.completedFuture(null);
//        }
        var currentState = context.state().getCurrentState();
        if (maxIterations <= 0 || !List.of(MONITORING_FOR_BREAKOUT, MONITORING_FOR_RETEST).contains(currentState)) {
            symbolContexts.remove(context.symbol());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return asyncOrbStrategy.onTick(context)
                .thenCompose(v -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .thenCompose(v -> onTick(context, maxIterations - 1));
    }

//    private CompletableFuture<Void> collectOpeningRangeDataSequentially(String symbol, String date, int count) {
//        if (count <= 0) {
//            return CompletableFuture.completedFuture(null);
//        }
//        return asyncOrbStrategy.startStrategy(new TradingContext(symbol, date, new SymbolTradingState(), StrategyMode.BACKTEST))
//                .thenCompose(v -> CompletableFuture.runAsync(() -> {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }))
//                .thenCompose(v -> collectOpeningRangeDataSequentially(symbol, date, count - 1));
//    }

    private CompletableFuture<Void> checkResult(String symbol, String date) {
        log.info("Checking profit");
        return this.checkMinute(symbol, date)
                .thenCompose(v -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .thenCompose(v -> checkResult(symbol, date));
    }

    private CompletableFuture<Void> checkMinute(String symbol, String date) {
        TwelveCandleBar oneMinBar;
        try {
            oneMinBar = dataClient.quoteWithInterval(symbol, TimeFrame.ONE_MIN, date);
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
