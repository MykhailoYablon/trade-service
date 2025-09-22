package com.example.tradeservice.strategy;

import com.example.tradeservice.configuration.TwelveDataClient;
import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.service.impl.PositionTracker;
import com.example.tradeservice.strategy.enums.TradingState;
import com.example.tradeservice.strategy.model.BreakoutData;
import com.example.tradeservice.strategy.model.OpeningRange;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.Types;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.example.tradeservice.strategy.utils.FileUtils.writeToLog;

@Service
@Slf4j
public class AsyncOpeningRangeBreakoutStrategy {

    // Configuration
    private static final List<String> SYMBOLS = List.of("GOOG", "AMZN");
    private static final int OPENING_RANGE_MINUTES = 15; // 9:30-9:45
    private static final int BREAKOUT_CONFIRMATION_BARS = 2;
    private static final BigDecimal RETEST_BUFFER = new BigDecimal("0.02");

    // Thread-safe state tracking for multiple symbols
    private final ConcurrentMap<String, SymbolTradingState> symbolStates = new ConcurrentHashMap<>();
    @Autowired
    private TwelveDataClient stockDataClient;

    @Autowired
    private OrderTracker orderTracker;
    @Autowired
    private PositionTracker positionTracker;
//    @Scheduled(cron = "0 56 16 * * MON-FRI", zone = "GMT+3") // Every 5 minutes from 9:30-9:44
//@Scheduled(cron = "0 56 16 * * MON-FRI")
//@Scheduled(cron = "0 1-6/5 17 * * MON-FRI")
    @Scheduled(cron = "0 36-50/5 16 * * MON-FRI", zone = "GMT+3") // Every 5 minutes from 9:30-9:44
    public void collectOpeningRangeDataForAllSymbols() {
        log.info("Starting opening range data collection for all symbols");
        List<CompletableFuture<Void>> futures = SYMBOLS.stream()
                .map(symbol -> this.collectOpeningRangeDataAsync(symbol, null))
                .toList();

        // Wait for all symbols to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Completed opening range collection for all symbols"));
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorAllSymbolsForBreakoutAndRetest() {
        List<CompletableFuture<Void>> futures = SYMBOLS.stream()
                .filter(this::shouldMonitorSymbol)
                .map(e -> this.monitorSymbolAsync(e, null))
                .toList();

        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> log.debug("Completed monitoring cycle for active symbols"));
        }
    }

    @Async
    public CompletableFuture<Void> collectOpeningRangeDataAsync(String symbol, String date) {

        date = Optional.ofNullable(date)
                        .orElseGet(() -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        log.info("{} - Starting to collect Opening Range Data {}", date, symbol);
        try {
            SymbolTradingState state = getOrCreateSymbolState(symbol + date);
            state.setTestDate(date);
            if (state.getCurrentState() != TradingState.COLLECTING_OPENING_RANGE) {
                initializeSymbolForNewTradingDay(symbol, state);
            }

            TwelveCandleBar fiveMinBar = fetchFiveMinuteCandle(symbol, date);
            if (fiveMinBar != null) {
                state.getFiveMinuteBars().add(fiveMinBar);

                log.info("[{}] Collected 5min bar {}/{}: High={}, Low={}, Close={}",
                        symbol, state.getFiveMinuteBars().size(), OPENING_RANGE_MINUTES / 5,
                        fiveMinBar.getHigh(), fiveMinBar.getLow(), fiveMinBar.getClose());

                // Check if we've collected enough bars for opening range
                if (state.getFiveMinuteBars().size() >= OPENING_RANGE_MINUTES / 5) {
                    calculateOpeningRange(symbol, state);
                    transitionToBreakoutMonitoring(symbol, state);
                }
            }
        } catch (Exception e) {
            log.error("[{}] - {} Error collecting opening range data", symbol, date, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> monitorSymbolAsync(String symbol, String date) {
        date = Optional.ofNullable(date)
                .orElseGet(() -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        try {
            SymbolTradingState state = symbolStates.get(symbol + date);
            if (state == null) return CompletableFuture.completedFuture(null);


            TwelveCandleBar oneMinBar = fetchOneMinuteCandle(symbol, date);
            if (oneMinBar != null) {

                if (state.getCurrentState() == TradingState.MONITORING_FOR_BREAKOUT) {
                    handleBreakoutMonitoring(symbol, state, oneMinBar);
                } else if (state.getCurrentState() == TradingState.MONITORING_FOR_RETEST) {
                    handleRetestMonitoring(symbol, state, oneMinBar);
                }

                writeToLog(symbol + "/" + state.getTestDate() + ".log",
                        String.format("[%s] One min bar added: Close=%s > Opening High=%s",
                                symbol, oneMinBar.getClose(), state.getOpeningRange().high()));
            }
        } catch (Exception e) {
            log.error("[{}] Error monitoring for breakout/retest", symbol, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    public boolean shouldMonitorSymbol(String symbol) {
        SymbolTradingState state = symbolStates.get(symbol);
        if (state == null) return false;

        return state.getCurrentState() == TradingState.MONITORING_FOR_BREAKOUT ||
                state.getCurrentState() == TradingState.MONITORING_FOR_RETEST;
    }

    private SymbolTradingState getOrCreateSymbolState(String symbol) {
        return symbolStates.computeIfAbsent(symbol, k -> new SymbolTradingState());
    }

    private void initializeSymbolForNewTradingDay(String symbol, SymbolTradingState state) {
        state.reset();
        state.setCurrentState(TradingState.COLLECTING_OPENING_RANGE);
        state.setMarketOpenTime(LocalDateTime.now());

        log.info("[{}] Initialized for new trading day at {}", symbol, state.getMarketOpenTime());
    }

    private void calculateOpeningRange(String symbol, SymbolTradingState state) {
        List<TwelveCandleBar> fiveMinuteBars = state.getFiveMinuteBars();
        if (fiveMinuteBars.isEmpty()) {
            log.warn("[{}] No 5-minute bars available to calculate opening range", symbol);
            return;
        }

        BigDecimal high = new BigDecimal(fiveMinuteBars.getFirst().getHigh());
        BigDecimal low = new BigDecimal(fiveMinuteBars.getFirst().getLow());

        for (TwelveCandleBar bar : fiveMinuteBars) {
            BigDecimal barHigh = new BigDecimal(bar.getHigh());
            if (barHigh.compareTo(high) > 0) {
                high = barHigh;
            }
            BigDecimal barLow = new BigDecimal(bar.getLow());
            if (barLow.compareTo(low) < 0) {
                low = barLow;
            }
        }

        state.setOpeningRange(new OpeningRange(high, low));
        log.info("[{}] Opening range calculated - High: {}, Low: {}, Range: {}",
                symbol, state.getOpeningRange().high(), state.getOpeningRange().low(),
                state.getOpeningRange().high().subtract(state.getOpeningRange().low()));

        // Write some sample lines to the log file
        new File("logs/" + symbol).mkdirs();
        writeToLog(symbol + "/" + state.getTestDate() + ".log",
                String.format("[%s] Opening range calculated - High: %s, Low: %s, Range: %s",
                        symbol, state.getOpeningRange().high(), state.getOpeningRange().low(),
                        state.getOpeningRange().high().subtract(state.getOpeningRange().low())));
    }

    private void transitionToBreakoutMonitoring(String symbol, SymbolTradingState state) {
        state.setCurrentState(TradingState.MONITORING_FOR_BREAKOUT);
        state.setBreakoutStartTime(LocalDateTime.now());
        state.getOneMinuteBreakoutBars().clear();

        log.info("[{}] Transitioning to breakout monitoring phase at {}", symbol, state.getBreakoutStartTime());
    }

    private void handleBreakoutMonitoring(String symbol, SymbolTradingState state, TwelveCandleBar oneMinBar) {
        OpeningRange openingRange = state.getOpeningRange();

        // Check for upward breakout
        String close = oneMinBar.getClose();
        BigDecimal high = openingRange.high();
        if (new BigDecimal(close).compareTo(high) > 0) {
            state.getOneMinuteBreakoutBars().add(oneMinBar);
            log.info("[{}] Potential breakout bar added: Close={} > Opening High={}",
                    symbol, close, high);

            writeToLog(symbol + "/" + state.getTestDate() + ".log",
                    String.format("[%s] Potential breakout bar added: Close=%s > Opening High=%s",
                            symbol, close, high));

            // Check if we have enough confirmation bars
            if (state.getOneMinuteBreakoutBars().size() >= BREAKOUT_CONFIRMATION_BARS) {
                if (isBreakoutConfirmed(state)) {
                    confirmBreakout(symbol, state, oneMinBar);
                }
            }
        } else {
            // Reset if price closes back below opening high
            log.info("Candle - {}, Close - {}, High - {}", oneMinBar.getDatetime(), close, high);
            if (!state.getOneMinuteBreakoutBars().isEmpty()) {
                log.info("[{}] Breakout attempt failed, resetting confirmation bars", symbol);
                state.getOneMinuteBreakoutBars().clear();
            }
        }
    }

    private boolean isBreakoutConfirmed(SymbolTradingState state) {
        OpeningRange openingRange = state.getOpeningRange();
        // All confirmation bars should close above opening high
        return state.getOneMinuteBreakoutBars().stream()
                .allMatch(bar -> new BigDecimal(bar.getClose()).compareTo(openingRange.high()) > 0);
    }

    private void confirmBreakout(String symbol, SymbolTradingState state, TwelveCandleBar breakoutBar) {
        BreakoutData breakoutData = new BreakoutData(
                new BigDecimal(breakoutBar.getClose()),
                LocalDateTime.now(),
                new BigDecimal(breakoutBar.getHigh())
        );

        state.setBreakoutData(breakoutData);
        state.setCurrentState(TradingState.MONITORING_FOR_RETEST);
        state.setRetestStartTime(LocalDateTime.now());


        // Write some sample lines to the log file
        BigDecimal openingHigh = state.getOpeningRange().high();
        writeToLog(symbol + "/break/" + state.getTestDate() + ".log",
                String.format("BREAKOUT %s with price - %s and high - %s",
                        symbol,
                        breakoutBar.getClose(), openingHigh));

        log.info("[{}] BREAKOUT CONFIRMED! Price: {}, Time: {}, Opening High: {}",
                symbol, breakoutData.breakoutPrice(), breakoutData.breakoutTime(),
                openingHigh);
    }

    private void handleRetestMonitoring(String symbol, SymbolTradingState state, TwelveCandleBar oneMinBar) {
        OpeningRange openingRange = state.getOpeningRange();
        BigDecimal retestLevel = openingRange.high().add(RETEST_BUFFER);

        // Check if low of candle stays above retest level (successful retest)
        // OR if close drops below opening high (deeper retest)
        if (new BigDecimal(oneMinBar.getLow()).compareTo(retestLevel) >= 0) {
            // Shallow retest - price held above breakout level
            log.info("[{}] SHALLOW RETEST DETECTED - Low: {} held above retest level: {}",
                    symbol, oneMinBar.getLow(), retestLevel);
            confirmRetestAndPrepareEntry(symbol, state, "SHALLOW");

        } else if (new BigDecimal(oneMinBar.getClose()).compareTo(openingRange.high()) <= 0) {
            // Deeper retest - price closed back below opening high
            log.info("[{}] DEEP RETEST DETECTED - Close: {} back below opening high: {}",
                    symbol, oneMinBar.getClose(), openingRange.high());
            confirmRetestAndPrepareEntry(symbol, state, "DEEP");
        }
    }

    private void confirmRetestAndPrepareEntry(String symbol, SymbolTradingState state, String retestType) {
        log.info("[{}] RETEST CONFIRMED ({})! Ready for entry logic.", symbol, retestType);

        OpeningRange openingRange = state.getOpeningRange();
        // Calculate suggested entry parameters
        BigDecimal suggestedEntry = openingRange.high().add(new BigDecimal("0.01"));
        BigDecimal suggestedStop = openingRange.low().subtract(new BigDecimal("0.01"));
        BigDecimal riskAmount = suggestedEntry.subtract(suggestedStop);

        log.info("[{}] ENTRY SETUP - Suggested Entry: {}, Stop Loss: {}, Risk per share: {}",
                symbol, suggestedEntry, suggestedStop, riskAmount);

        String testDate = state.getTestDate();
        writeToLog(symbol + "/break/" + testDate + ".log",
                String.format("RETEST %s with retestType - %s and entry price - %s and stop loss price - %s", symbol,
                        retestType, suggestedEntry, suggestedStop));

        // TODO: Implement your buy logic and risk management here
        processEntryAsync(symbol, suggestedEntry, suggestedStop, testDate);

        state.setCurrentState(TradingState.SETUP_COMPLETE);
    }

    // Async helper method for order processing
    @Async
    public CompletableFuture<Void> processEntryAsync(String symbol, BigDecimal entryPrice,
                                                     BigDecimal stopPrice, String testDate) {
        try {
            // TODO: Implement your actual order placement logic here
            log.info("[{}] Processing entry order - Entry: {}, Stop: {}",
                    symbol, entryPrice, stopPrice);

            // Write some sample lines to the log file


            // calculatePositionSize(riskAmount);

            Contract contract = positionTracker.getPositionBySymbol(symbol).getContract();

            List<Order> orders = orderTracker.placeMarketOrder(contract, Types.Action.BUY, 100, stopPrice);

            orders.forEach(order -> writeToLog(symbol + "/break/" + testDate + ".log",
                    String.format("%s ORDER with type %s has been placed",
                            order.getAction(), order.getOrderType())));

            log.info("[{}] Entry order processed successfully", symbol);
        } catch (Exception e) {
            log.error("[{}] Error processing entry order", symbol, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    // Mock methods - replace with your actual data fetching logic
    private TwelveCandleBar fetchFiveMinuteCandle(String symbol, String date) {
        return stockDataClient.quoteWithInterval(symbol, TimeFrame.FIVE_MIN, date);
    }

    private TwelveCandleBar fetchOneMinuteCandle(String symbol, String date) {
        return stockDataClient.quoteWithInterval(symbol, TimeFrame.ONE_MIN, date);
    }

    public boolean isBreak(String symbol) {
        SymbolTradingState state = symbolStates.get(symbol);
        if (state == null) return false;

        return state.getCurrentState() == TradingState.SETUP_COMPLETE;
    }
}
