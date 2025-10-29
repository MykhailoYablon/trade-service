package com.example.tradeservice.backtest;

import com.example.tradeservice.backtest.series.DoubleSeries;
import com.example.tradeservice.service.csv.CsvService;
import com.example.tradeservice.strategy.AsyncTradingStrategy;
import com.example.tradeservice.strategy.enums.StrategyDataSource;
import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.enums.StrategyType;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.tradeservice.strategy.enums.TradingState.MONITORING_FOR_BREAKOUT;
import static com.example.tradeservice.strategy.enums.TradingState.MONITORING_FOR_RETEST;

@Slf4j
@Service
public class BacktestTradingStrategy {

    // Thread-safe state tracking for multiple symbols
    private final Map<String, TradingContext> symbolContexts = new ConcurrentHashMap<>();

    private final Map<StrategyType, AsyncTradingStrategy> strategies;
    private final CsvService csvService;

    public BacktestTradingStrategy(List<AsyncTradingStrategy> strategyList,
                                   CsvService csvService) {
        this.csvService = csvService;
        strategies = strategyList.stream()
                .filter(str -> str.getStrategyDataSource().equals(StrategyDataSource.CSV))
                .collect(Collectors.toMap(
                        AsyncTradingStrategy::getStrategyType,
                        Function.identity()
                ));


    }

    public void test(String symbol, StrategyType strategyType, String from, String to) {
        var strategy = strategies.get(strategyType);
        // initializing csv and Redis candles
        DoubleSeries series;
        //
        if (StrategyType.BUY_AND_HOLD.equals(strategyType)) {
            LocalDate fromDate = Optional.ofNullable(from)
                    .map(LocalDate::parse)
                    .orElse(null);
            LocalDate toDate = Optional.ofNullable(to)
                    .map(LocalDate::parse)
                    .orElse(null);
            series = csvService.readDoubleSeries(symbol, fromDate, toDate);
        } else
            series = csvService.initializeCsvForDay(symbol, "2025-09-05");

        int deposit = 15000;
        Backtest backtest = new Backtest(deposit, series, symbol);
        backtest.setLeverage(4);

        // do the backtest
        Backtest.Result result = backtest.run(strategy);

        log.info("Result - {}", result.pl);
    }

    @Async("strategyExecutor")
    public CompletableFuture<List<Order>> startStrategy(String symbol, StrategyType strategyType, String date) {
        var strategy = strategies.get(strategyType);
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
        var strategy = strategies.get(strategyType);
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
