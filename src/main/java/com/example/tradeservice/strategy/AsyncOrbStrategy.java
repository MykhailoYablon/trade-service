package com.example.tradeservice.strategy;

import com.example.tradeservice.model.PositionHolder;
import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.service.impl.PositionTracker;
import com.example.tradeservice.strategy.dataclient.StockDataClient;
import com.example.tradeservice.strategy.enums.TradingState;
import com.example.tradeservice.strategy.model.BreakoutData;
import com.example.tradeservice.strategy.model.OpeningRange;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.Types;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.example.tradeservice.strategy.enums.StrategyMode.LIVE;
import static com.example.tradeservice.strategy.enums.TradingState.MONITORING_FOR_BREAKOUT;
import static com.example.tradeservice.strategy.enums.TradingState.MONITORING_FOR_RETEST;
import static com.example.tradeservice.strategy.utils.FileUtils.writeToLog;

@Component
@Slf4j
public class AsyncOrbStrategy implements AsyncTradingStrategy {

    // Configuration
    private static final int OPENING_RANGE_MINUTES = 15; // 9:30-9:45
    private static final int BREAKOUT_CONFIRMATION_BARS = 2;
    private static final BigDecimal RETEST_BUFFER = new BigDecimal("0.02");

    private final StockDataClient dataClient;
    private final OrderTracker orderTracker;
    private final PositionTracker positionTracker;

    public AsyncOrbStrategy(StockDataClient dataClient, OrderTracker orderTracker, PositionTracker positionTracker) {
        this.dataClient = dataClient;
        this.orderTracker = orderTracker;
        this.positionTracker = positionTracker;
    }

    @Async("strategyExecutor")
    @Override
    public CompletableFuture<TradingContext> startStrategy(TradingContext context) {
        var date = context.getDate();
        var symbol = context.getSymbol();
        date = Optional.ofNullable(date)
                .orElseGet(() -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        try {
            SymbolTradingState state = context.getState();
            if (state.getCurrentState() != TradingState.COLLECTING_OPENING_RANGE) {
                initializeSymbolForNewTradingDay(symbol, state);
            }
            state.setTestDate(date);

            for (int i = 0; i < 3; i++) {
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
                log.info("{} [{}] Waiting 5 minutes before next interval", date, symbol);
                if (LIVE.equals(context.getMode()) && i < 2)
                    Thread.sleep(5 * 60 * 1000); // 5 minutes
            }
        } catch (Exception e) {
            log.error("[{}] - {} Error collecting opening range data", symbol, date, e);
        }

        return CompletableFuture.completedFuture(context);
    }

    @Override
    @Async("strategyExecutor")
    public CompletableFuture<List<Order>> onTick(TradingContext context) {
        if (Objects.isNull(context)) {
            log.info("Context has not been initialized yet");
            return CompletableFuture.completedFuture(Collections.emptyList());
        } else if (!List.of(MONITORING_FOR_BREAKOUT, MONITORING_FOR_RETEST)
                .contains(context.getState().getCurrentState())) {
            log.info("Current state is not for monitoring yet");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        String date = context.getDate();
        var symbol = context.getSymbol();
        log.info("Context onTick = {}, {}, {}", context.getSymbol(), context.getDate(), context.getState().getCurrentState());

        date = Optional.ofNullable(date)
                .orElseGet(() -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        try {
            SymbolTradingState state = context.getState();
            if (state == null) return CompletableFuture.completedFuture(Collections.emptyList());

            TwelveCandleBar oneMinBar = fetchOneMinuteCandle(symbol, date);
            if (oneMinBar != null) {
                if (state.getCurrentState() == MONITORING_FOR_BREAKOUT) {
                    handleBreakoutMonitoring(symbol, state, oneMinBar);
                } else if (state.getCurrentState() == MONITORING_FOR_RETEST) {
                    return handleRetestMonitoring(context, oneMinBar);
                }

                writeToLog(symbol + "/" + state.getTestDate() + ".log",
                        String.format("[%s] One min bar added: Close=%s > Opening High=%s",
                                symbol, oneMinBar.getClose(), state.getOpeningRange().high()));
            }
        } catch (Exception e) {
            log.error("[{}] Error monitoring for breakout/retest", symbol, e);
        }

        return CompletableFuture.completedFuture(Collections.emptyList());
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
        state.setCurrentState(MONITORING_FOR_BREAKOUT);
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
        state.setCurrentState(MONITORING_FOR_RETEST);
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

    private CompletableFuture<List<Order>> handleRetestMonitoring(TradingContext context, TwelveCandleBar oneMinBar) {
        SymbolTradingState state = context.getState();
        String symbol = context.getSymbol();
        OpeningRange openingRange = state.getOpeningRange();
        BigDecimal retestLevel = openingRange.high().add(RETEST_BUFFER);

        // Check if low of candle stays above retest level (successful retest)
        // OR if close drops below opening high (deeper retest)
        CompletableFuture<List<Order>> orders = new CompletableFuture<>();
        if (new BigDecimal(oneMinBar.getLow()).compareTo(retestLevel) >= 0) {
            // Shallow retest - price held above breakout level
            log.info("[{}] SHALLOW RETEST DETECTED - Low: {} held above retest level: {}",
                    symbol, oneMinBar.getLow(), retestLevel);
            orders = confirmRetestAndPrepareEntry(symbol, context, "SHALLOW");


        } else if (new BigDecimal(oneMinBar.getClose()).compareTo(openingRange.high()) <= 0) {
            // Deeper retest - price closed back below opening high
            log.info("[{}] DEEP RETEST DETECTED - Close: {} back below opening high: {}",
                    symbol, oneMinBar.getClose(), openingRange.high());
            orders = confirmRetestAndPrepareEntry(symbol, context, "DEEP");
        }
        return orders;
    }

    private CompletableFuture<List<Order>> confirmRetestAndPrepareEntry(String symbol, TradingContext context,
                                                                        String retestType) {
        log.info("[{}] RETEST CONFIRMED ({})! Ready for entry logic.", symbol, retestType);
        var state = context.getState();
        OpeningRange openingRange = state.getOpeningRange();
        // Calculate suggested entry parameters
        BigDecimal suggestedEntry = openingRange.high().add(new BigDecimal("0.01"));
        BigDecimal stopPrice = openingRange.low().subtract(new BigDecimal("0.01"));
        BigDecimal riskAmount = suggestedEntry.subtract(stopPrice);

        log.info("[{}] ENTRY SETUP - Suggested Entry: {}, Stop Loss: {}, Risk per share: {}",
                symbol, suggestedEntry, stopPrice, riskAmount);

        String testDate = state.getTestDate();
        writeToLog(symbol + "/break/" + testDate + ".log",
                String.format("RETEST %s with retestType - %s and entry price - %s and stop loss price - %s", symbol,
                        retestType, suggestedEntry, stopPrice));

        CompletableFuture<List<Order>> orders = processEntryAsync(symbol, suggestedEntry, stopPrice, testDate);

        //we need to adjust orders calculation. I mean profit will be taken only on certain price
        context.order(symbol, true, 100, stopPrice);

        state.setCurrentState(TradingState.SETUP_COMPLETE);
        return orders;
    }

    // Async helper method for order processing
    @Async("strategyExecutor")
    public CompletableFuture<List<Order>> processEntryAsync(String symbol, BigDecimal entryPrice,
                                                            BigDecimal stopPrice, String testDate) {
        try {
            log.info("[{}] Processing entry order - Entry: {}, Stop: {}",
                    symbol, entryPrice, stopPrice);

            Contract contract = Optional.ofNullable(positionTracker.getPositionBySymbol(symbol))
                    .map(PositionHolder::getContract)
                    .orElse(null);


            List<Order> orders = orderTracker.placeMarketOrder(Types.Action.BUY, 100, stopPrice);
//            List<Order> orders = orderTracker.placeMarketOrder(contract, Types.Action.BUY, 100, stopPrice);

            orders.forEach(order -> writeToLog(symbol + "/break/" + testDate + ".log",
                    String.format("%s ORDER with type %s has been placed and order lmtPrice - %s, ",
                            order.getAction(), order.getOrderType(), order.lmtPrice())));
            log.info("[{}] Entry orders processed successfully", symbol);
            return CompletableFuture.completedFuture(orders);
        } catch (Exception e) {
            log.error("[{}] Error processing entry order", symbol, e);
        }

        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    // Mock methods - replace with your actual data fetching logic
    private TwelveCandleBar fetchFiveMinuteCandle(String symbol, String date) {
        return dataClient.quoteWithInterval(symbol, TimeFrame.FIVE_MIN, date);
    }

    private TwelveCandleBar fetchOneMinuteCandle(String symbol, String date) {
        return dataClient.quoteWithInterval(symbol, TimeFrame.ONE_MIN, date);
    }
}
