package com.example.tradeservice.strategy.scheduled;

import com.example.tradeservice.strategy.AsyncTradingStrategy;
import com.example.tradeservice.strategy.dataclient.StockDataClient;
import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ScheduledStrategy {

    // Thread-safe state tracking for multiple symbols
    private final Map<String, TradingContext> symbolContexts = new ConcurrentHashMap<>();


    private static final List<String> SYMBOLS = List.of("GOOGL", "AMZN", "MSFT");

    @Autowired
    @Qualifier("twelveDataStrategy")
    private AsyncTradingStrategy asyncOrbStrategy;

    //@Scheduled(cron = "0 56 16 * * MON-FRI", zone = "GMT+3") // Every 5 minutes from 9:30-9:44
    //@Scheduled(cron = "0 56 16 * * MON-FRI")
    //@Scheduled(cron = "0 1-6/5 17 * * MON-FRI")
    @Scheduled(cron = "0 44 16 * * MON-FRI", zone = "GMT+3")
    public void collectOpeningRangeDataForAllSymbols() {
        log.info("Starting opening range data collection for all symbols");
        List<CompletableFuture<TradingContext>> futures = SYMBOLS.stream()
                .map(symbol -> asyncOrbStrategy.startStrategy(new TradingContext(symbol, null,
                        new SymbolTradingState(), StrategyMode.LIVE)))
                .toList();

        // Wait for all symbols to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    // Collect all completed contexts
                    futures.forEach(future -> {
                        try {
                            TradingContext completedContext = future.join();
                            if (completedContext != null) {
                                symbolContexts.put(completedContext.symbol(), completedContext);
                                log.info("Saved context for symbol: {}", completedContext.symbol());
                            }
                        } catch (Exception e) {
                            log.error("Error retrieving context from future", e);
                        }
                    });
                    log.info("Completed initial 5 min opening range collection for all symbols");
                });
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorAllSymbolsForBreakoutAndRetest() {
        List<CompletableFuture<List<Order>>> futures = SYMBOLS.stream()
                .filter(symbol -> Objects.nonNull(symbolContexts.get(symbol))
                        && asyncOrbStrategy.shouldMonitorSymbol(symbolContexts.get(symbol).state()))
                .map(symbol -> asyncOrbStrategy.onTick(symbolContexts.get(symbol)))
                .toList();

        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> log.debug("Completed monitoring cycle for active symbols"));
        }
    }
}
