package com.example.tradeservice.strategy.scheduled;

import com.example.tradeservice.strategy.AsyncTradingStrategy;
import com.example.tradeservice.strategy.dataclient.StockDataClient;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ScheduledStrategy {

    private static final List<String> SYMBOLS = List.of("GOOG", "AMZN", "MSFT");

    @Autowired
    @Qualifier("twelveDataStrategy")
    private AsyncTradingStrategy asyncOrbStrategy;

    //@Scheduled(cron = "0 56 16 * * MON-FRI", zone = "GMT+3") // Every 5 minutes from 9:30-9:44
    //@Scheduled(cron = "0 56 16 * * MON-FRI")
    //@Scheduled(cron = "0 1-6/5 17 * * MON-FRI")
    @Scheduled(cron = "0 0-50/5 11 * * MON-FRI", zone = "GMT+3") // Every 5 minutes from 9:30-9:44
    public void collectOpeningRangeDataForAllSymbols() {
        log.info("Starting opening range data collection for all symbols");
        List<CompletableFuture<Void>> futures = SYMBOLS.stream()
                .map(symbol -> asyncOrbStrategy.startStrategy(new TradingContext(symbol, null, new SymbolTradingState())))
                .toList();

        // Wait for all symbols to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Completed initial 5 min opening range collection for all symbols"));
    }

//    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorAllSymbolsForBreakoutAndRetest() {
        List<CompletableFuture<Void>> futures = SYMBOLS.stream()
                .filter(symbol -> asyncOrbStrategy.shouldMonitorSymbol())
                .map(e -> asyncOrbStrategy.onTick())
                .toList();

        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> log.debug("Completed monitoring cycle for active symbols"));
        }
    }
}
