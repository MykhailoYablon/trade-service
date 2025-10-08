package com.example.tradeservice.backtest;

import com.example.tradeservice.backtest.series.DoubleSeries;
import com.example.tradeservice.service.csv.CsvService;
import com.example.tradeservice.service.csv.CsvServiceImpl;
import com.example.tradeservice.strategy.AsyncTradingStrategy;
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
public class BacktestTradingStrategy {

    // Thread-safe state tracking for multiple symbols
    private final Map<String, TradingContext> symbolContexts = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("csvDataStrategy")
    private AsyncTradingStrategy strategy;

    @Autowired
    @Qualifier("csvData")
    private StockDataClient dataClient;

    @Autowired
    private CsvService csvService;

    @Autowired
    private CsvServiceImpl csvServiceImpl;

    public void test(String symbol) {

        // initializing csv and Redis candles but
        DoubleSeries series = csvService.initializeCsvForDay(symbol, "2025-09-05");
        //
//        DoubleSeries doubleSeries = csvServiceImpl.readDoubleSeries(symbol);

        int deposit = 15000;
        Backtest backtest = new Backtest(deposit, series, symbol);
        backtest.setLeverage(4);

        // do the backtest
        Backtest.Result result = backtest.run(strategy);

        log.info("Result - {}", result.pl);
    }

    @Async("strategyExecutor")
    public CompletableFuture<List<Order>> startStrategy(String symbol, String date) {
        new File("logs/" + symbol + "/break").mkdirs();
        TradingContext context = TradingContext.builder()
                .symbol(symbol)
                .date(date)
                .state(new SymbolTradingState())
                .mode(StrategyMode.BACKTEST)
                .build();
        return CompletableFuture
                .runAsync(() -> csvService.initializeCsvForDay(symbol, date))
                .thenCompose(v -> strategy.startStrategy(context))
                .thenCompose(ctx -> {
                    if (ctx != null) {
//                        symbolContexts.put(ctx.getSymbol(), ctx);
                        log.info("Saved context for symbol: {}", ctx.getSymbol());
                    }
                    return onTick(ctx, 100);
                })
                .exceptionally(e -> {
                    log.error("[{}] Error monitoring for breakout/retest", symbol, e);
                    return null;
                });
    }

    public CompletableFuture<List<Order>> onTick(TradingContext context, int maxIterations) {

        var currentState = context.getState().getCurrentState();
        if (maxIterations <= 0 || !List.of(MONITORING_FOR_BREAKOUT, MONITORING_FOR_RETEST).contains(currentState)) {
//            symbolContexts.remove(context.getSymbol());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return strategy.onTick(context)
                .thenCompose(v -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }))
                .thenCompose(v -> onTick(context, maxIterations - 1));
    }

}
