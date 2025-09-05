package com.example.tradeservice.strategy;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.strategy.enums.TradingState;
import com.example.tradeservice.strategy.model.BreakoutData;
import com.example.tradeservice.strategy.model.OpeningRange;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.example.tradeservice.strategy.StrategyService.createLogFileName;
import static com.example.tradeservice.strategy.StrategyService.writeToLog;

@Service
@Slf4j
public class AsyncOpeningRangeBreakoutService {

    // Configuration
    private static final List<String> SYMBOLS = List.of("MSFT", "GOOGL", "exports/AMZN");
    private static final int OPENING_RANGE_MINUTES = 15; // 9:30-9:45
    private static final int BREAKOUT_CONFIRMATION_BARS = 2;
    private static final BigDecimal RETEST_BUFFER = new BigDecimal("0.02");
    private static final int MAX_BREAKOUT_WAIT_MINUTES = 120;
    private static final int MAX_RETEST_WAIT_MINUTES = 30;

    // Thread-safe state tracking for multiple symbols
    private final ConcurrentMap<String, SymbolTradingState> symbolStates = new ConcurrentHashMap<>();
    @Autowired
    private CsvStockDataClient stockDataClient;

    @Scheduled(cron = "0 36-50/5 18 * * MON-FRI", zone = "GMT+3") // Every 5 minutes from 9:30-9:44
    public void collectOpeningRangeDataForAllSymbols() {
        log.info("Starting opening range data collection for all symbols");

        List<CompletableFuture<Void>> futures = SYMBOLS.stream()
                .map(this::collectOpeningRangeDataAsync)
                .toList();

        // Wait for all symbols to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Completed opening range collection for all symbols"));
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorAllSymbolsForBreakoutAndRetest() {
        List<CompletableFuture<Void>> futures = SYMBOLS.stream()
                .filter(this::shouldMonitorSymbol)
                .map(this::monitorSymbolAsync)
                .toList();

        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> log.debug("Completed monitoring cycle for active symbols"));
        }
    }

    @Async
    public CompletableFuture<Void> collectOpeningRangeDataAsync(String symbol) {
        try {
            SymbolTradingState state = getOrCreateSymbolState(symbol);

            if (state.getCurrentState() != TradingState.COLLECTING_OPENING_RANGE) {
                initializeSymbolForNewTradingDay(symbol, state);
            }

            TwelveCandleBar fiveMinBar = fetchFiveMinuteCandle(symbol);
            if (fiveMinBar != null) {
                state.getFiveMinuteBars().add(fiveMinBar);

                log.info("[{}] Collected 5min bar {}/{}: High={}, Low={}, Close={}",
                        symbol, state.getFiveMinuteBars().size(), OPENING_RANGE_MINUTES/5,
                        fiveMinBar.getHigh(), fiveMinBar.getLow(), fiveMinBar.getClose());

                // Check if we've collected enough bars for opening range
                if (state.getFiveMinuteBars().size() >= OPENING_RANGE_MINUTES/5) {
                    calculateOpeningRange(symbol, state);
                    transitionToBreakoutMonitoring(symbol, state);
                }
            }
        } catch (Exception e) {
            log.error("[{}] Error collecting opening range data", symbol, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> monitorSymbolAsync(String symbol) {
        try {
            SymbolTradingState state = symbolStates.get(symbol);
            if (state == null) return CompletableFuture.completedFuture(null);

            TwelveCandleBar oneMinBar = fetchOneMinuteCandle(symbol);
            if (oneMinBar != null) {

                if (state.getCurrentState() == TradingState.MONITORING_FOR_BREAKOUT) {
                    handleBreakoutMonitoring(symbol, state, oneMinBar);
                } else if (state.getCurrentState() == TradingState.MONITORING_FOR_RETEST) {
                    handleRetestMonitoring(symbol, state, oneMinBar);
                }

                // Check timeouts for this symbol
                checkForTimeouts(symbol, state);
            }
        } catch (Exception e) {
            log.error("[{}] Error monitoring for breakout/retest", symbol, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private boolean shouldMonitorSymbol(String symbol) {
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
        if (new BigDecimal(oneMinBar.getClose()).compareTo(openingRange.high()) > 0) {
            state.getOneMinuteBreakoutBars().add(oneMinBar);
            log.info("[{}] Potential breakout bar added: Close={} > Opening High={}",
                    symbol, oneMinBar.getClose(), openingRange.high());

            // Check if we have enough confirmation bars
            if (state.getOneMinuteBreakoutBars().size() >= BREAKOUT_CONFIRMATION_BARS) {
                if (isBreakoutConfirmed(state)) {
                    confirmBreakout(symbol, state, oneMinBar);
                }
            }
        } else {
            // Reset if price closes back below opening high
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

        String logFileName = createLogFileName("OpeningBreakRange-");

        // Write some sample lines to the log file
        writeToLog(logFileName, String.format("BREAKOUT %s with price - %s and high - %s", symbol,
                breakoutBar.getClose(), breakoutData.breakoutHigh()));

        log.info("[{}] BREAKOUT CONFIRMED! Price: {}, Time: {}, Opening High: {}",
                symbol, breakoutData.breakoutPrice(), breakoutData.breakoutTime(),
                state.getOpeningRange().high());
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

        // TODO: Implement your buy logic and risk management here
         processEntryAsync(symbol, suggestedEntry, suggestedStop, riskAmount);

        state.setCurrentState(TradingState.SETUP_COMPLETE);
    }

    private void checkForTimeouts(String symbol, SymbolTradingState state) {
        LocalDateTime now = LocalDateTime.now();

        if (state.getCurrentState() == TradingState.MONITORING_FOR_BREAKOUT &&
                state.getBreakoutStartTime() != null) {
            if (state.getBreakoutStartTime().plusMinutes(MAX_BREAKOUT_WAIT_MINUTES).isBefore(now)) {
                log.info("[{}] Breakout timeout reached, no valid breakout occurred within {} minutes",
                        symbol, MAX_BREAKOUT_WAIT_MINUTES);
                state.setCurrentState(TradingState.TIMEOUT);
            }
        }

        if (state.getCurrentState() == TradingState.MONITORING_FOR_RETEST &&
                state.getRetestStartTime() != null) {
            if (state.getRetestStartTime().plusMinutes(MAX_RETEST_WAIT_MINUTES).isBefore(now)) {
                log.info("[{}] Retest timeout reached, no valid retest occurred within {} minutes",
                        symbol, MAX_RETEST_WAIT_MINUTES);
                state.setCurrentState(TradingState.TIMEOUT);
            }
        }
    }

    // Async helper method for order processing
    @Async
    public CompletableFuture<Void> processEntryAsync(String symbol, BigDecimal entryPrice,
                                                     BigDecimal stopPrice, BigDecimal riskAmount) {
        try {
            // TODO: Implement your actual order placement logic here
            log.info("[{}] Processing entry order - Entry: {}, Stop: {}, Risk: {}",
                    symbol, entryPrice, stopPrice, riskAmount);

            String logFileName = createLogFileName("Retest-");

            // Write some sample lines to the log file
            writeToLog(logFileName, String.format("RETEST %s with entry price - %s and stop loss price - %s", symbol,
                    entryPrice, stopPrice));

            // Lets say 100 shares
            // calculatePositionSize(riskAmount);
            var positionSize = 100;

            log.info("[{}] Entry order processed successfully", symbol);
        } catch (Exception e) {
            log.error("[{}] Error processing entry order", symbol, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    // Mock methods - replace with your actual data fetching logic
    private TwelveCandleBar fetchFiveMinuteCandle(String symbol) {
        return stockDataClient.quoteWithInterval(symbol, TimeFrame.FIVE_MIN);
    }

    private TwelveCandleBar fetchOneMinuteCandle(String symbol) {
        return stockDataClient.quoteWithInterval(symbol, TimeFrame.ONE_MIN);
    }

    // Public methods for monitoring and control
    public void addSymbol(String symbol) {
        if (!SYMBOLS.contains(symbol)) {
            log.warn("Symbol {} not in configured symbol list", symbol);
            return;
        }

        SymbolTradingState state = getOrCreateSymbolState(symbol);
        log.info("[{}] Symbol added for monitoring", symbol);
    }

    public void removeSymbol(String symbol) {
        symbolStates.remove(symbol);
        log.info("[{}] Symbol removed from monitoring", symbol);
    }

    public TradingState getSymbolState(String symbol) {
        SymbolTradingState state = symbolStates.get(symbol);
        return state != null ? state.getCurrentState() : null;
    }

    public OpeningRange getSymbolOpeningRange(String symbol) {
        SymbolTradingState state = symbolStates.get(symbol);
        return state != null ? state.getOpeningRange() : null;
    }

    public BreakoutData getSymbolBreakoutData(String symbol) {
        SymbolTradingState state = symbolStates.get(symbol);
        return state != null ? state.getBreakoutData() : null;
    }

    public List<String> getActiveSymbols() {
        return symbolStates.entrySet().stream()
                .filter(entry -> entry.getValue().getCurrentState() != TradingState.TIMEOUT &&
                        entry.getValue().getCurrentState() != TradingState.SETUP_COMPLETE)
                .map(Map.Entry::getKey)
                .toList();
    }

    public void resetAllSymbols() {
        symbolStates.clear();
        log.info("All symbol states reset");
    }

    // Helper class to manage per-symbol state
    @Getter
    private static class SymbolTradingState {
        // Getters and setters
        @Setter
        private TradingState currentState = TradingState.WAITING_FOR_MARKET_OPEN;
        @Setter
        private OpeningRange openingRange;
        @Setter
        private BreakoutData breakoutData;
        private final List<TwelveCandleBar> fiveMinuteBars = new ArrayList<>();
        private final List<TwelveCandleBar> oneMinuteBreakoutBars = new ArrayList<>();
        @Setter
        private LocalDateTime marketOpenTime;
        @Setter
        private LocalDateTime breakoutStartTime;
        @Setter
        private LocalDateTime retestStartTime;

        public void reset() {
            currentState = TradingState.WAITING_FOR_MARKET_OPEN;
            openingRange = null;
            breakoutData = null;
            fiveMinuteBars.clear();
            oneMinuteBreakoutBars.clear();
            marketOpenTime = null;
            breakoutStartTime = null;
            retestStartTime = null;
        }

    }


}
