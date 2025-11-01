package com.strategy.trade.backtest;

import com.strategy.trade.backtest.series.DoubleSeries;
import com.strategy.trade.backtest.series.TimeSeries;
import com.strategy.trade.service.csv.CsvService;
import com.strategy.trade.strategy.AsyncTradingStrategy;
import com.strategy.trade.strategy.dataclient.TwelveDataClient;
import com.strategy.trade.strategy.enums.StrategyDataSource;
import com.strategy.trade.strategy.enums.StrategyMode;
import com.strategy.trade.strategy.enums.StrategyType;
import com.strategy.trade.strategy.model.SymbolTradingState;
import com.strategy.trade.strategy.model.TradingContext;
import com.ib.client.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.strategy.trade.strategy.enums.TradingState.MONITORING_FOR_BREAKOUT;
import static com.strategy.trade.strategy.enums.TradingState.MONITORING_FOR_RETEST;

@Slf4j
@Service
public class BacktestTradingStrategy {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Map<StrategyType, AsyncTradingStrategy> strategies;
    private final CsvService csvService;
    private final TwelveDataClient twelveDataClient;

    public BacktestTradingStrategy(List<AsyncTradingStrategy> strategyList,
                                   CsvService csvService, TwelveDataClient twelveDataClient) {
        this.csvService = csvService;
        strategies = strategyList.stream()
                .filter(str -> StrategyDataSource.CSV.equals(str.getStrategyDataSource()))
                .collect(Collectors.toMap(
                        AsyncTradingStrategy::getStrategyType,
                        Function.identity()
                ));
        this.twelveDataClient = twelveDataClient;
    }

    public Backtest.Result startBacktest(String symbol, StrategyType strategyType, String startDate, String endDate) {
        var strategy = strategies.get(strategyType);
        DoubleSeries series = getDoubleSeriesFromClient(symbol, strategyType, startDate, endDate);
        log.info("Series size = {}", series.getData().size());
        int deposit = 20000;
        Backtest backtest = new Backtest(deposit, series, symbol);
        backtest.setLeverage(4);

        // do the backtest
        Backtest.Result result = backtest.run(strategy);

        log.info("Result - {}", result.pl);
        return result;
    }

    //execute separate strategy
    @Async("strategyExecutor")
    public CompletableFuture<List<Order>> startStrategy(String symbol, String date) {
        var strategy = strategies.get(StrategyType.ORB);

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
                    log.info("Saved context for symbol: {}", ctx.getSymbol());
                    return onTick(ctx, strategy, 100);
                })
                .exceptionally(e -> {
                    log.error("[{}] Error monitoring for breakout/retest", symbol, e);
                    return null;
                });
    }

    public CompletableFuture<List<Order>> onTick(TradingContext context,
                                                 AsyncTradingStrategy strategy,
                                                 int maxIterations) {
        var currentState = context.getState().getCurrentState();
        if (maxIterations <= 0 || !List.of(MONITORING_FOR_BREAKOUT, MONITORING_FOR_RETEST).contains(currentState)) {
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
                .thenCompose(v -> onTick(context, strategy, maxIterations - 1));
    }


    private DoubleSeries getDoubleSeriesFromClient(String symbol, StrategyType strategyType, String startDate, String endDate) {
        DoubleSeries series;
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();

        if (StrategyType.BUY_AND_HOLD.equals(strategyType)) {
            String csv = twelveDataClient.csvTimeSeries(symbol, startDate, endDate);
            csv.lines()
                    .skip(1)
                    .forEach(line -> {
                        String[] parts = line.split(";");
                        // Parse datetime
                        Instant instant = LocalDate.parse(parts[0], DAY_FORMATTER)
                                .atStartOfDay(ZoneOffset.UTC).toInstant();
                        // Parse close value
                        Double closeValue = Double.parseDouble(parts[4]);
                        entries.add(new TimeSeries.Entry<>(closeValue, instant));
                    });
            series = new DoubleSeries(entries, symbol).toAscending();
        } else
            series = csvService.initializeCsvForDay(symbol, "2025-09-05");
        return series;
    }

    private DoubleSeries getDoubleSeriesFromCsv(String symbol, StrategyType strategyType, String from, String to) {
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
        return series;
    }

}
