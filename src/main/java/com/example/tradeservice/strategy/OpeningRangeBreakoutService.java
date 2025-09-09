package com.example.tradeservice.strategy;

import com.example.tradeservice.configuration.TwelveDataClient;
import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.strategy.enums.TradingState;
import com.example.tradeservice.strategy.model.BreakoutData;
import com.example.tradeservice.strategy.model.OpeningRange;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.tradeservice.strategy.StrategyService.createLogFileName;
import static com.example.tradeservice.strategy.StrategyService.writeToLog;

@Slf4j
@Service
public class OpeningRangeBreakoutService {


    // Configuration
    private static final String SYMBOL = "GOOG"; // Configure your symbol
    private static final int OPENING_RANGE_MINUTES = 15; // 9:30-9:45
    private static final int BREAKOUT_CONFIRMATION_BARS = 2; // Number of 1min bars to confirm breakout
    private static final BigDecimal RETEST_BUFFER = new BigDecimal("0.02"); // $0.02 buffer for retest
    private static final int MAX_BREAKOUT_WAIT_MINUTES = 120; // 2 hours max to wait for breakout
    private static final int MAX_RETEST_WAIT_MINUTES = 30; // 30 minutes max to wait for retest

    // Getter methods for monitoring
    // State tracking
    @Getter
    private TradingState currentState = TradingState.WAITING_FOR_MARKET_OPEN;
    @Getter
    private OpeningRange openingRange;
    @Getter
    private BreakoutData breakoutData;
    private final List<TwelveCandleBar> fiveMinuteBars = new ArrayList<>();
    private final List<TwelveCandleBar> oneMinuteBreakoutBars = new ArrayList<>();
    private LocalDateTime marketOpenTime;
    private LocalDateTime breakoutStartTime;
    private LocalDateTime retestStartTime;

    @Autowired
    private TwelveDataClient twelveDataClient;

    // Scheduled tasks
//    @Scheduled(cron = "0 31-45/5 16 * * MON-FRI", zone = "GMT+3") // Every 5 minutes from 9:30-9:44
    public void collectOpeningRangeData() {
        if (currentState != TradingState.COLLECTING_OPENING_RANGE) {
            initializeForNewTradingDay();
        }

        try {
            String date = LocalDate.now().toString();
            TwelveCandleBar fiveMinBar = fetchFiveMinuteCandle(SYMBOL, date);
            if (fiveMinBar != null) {
                fiveMinuteBars.add(fiveMinBar);
                log.info("Collected 5min bar {}/{}: High={}, Low={}, Close={}",
                        fiveMinuteBars.size(), OPENING_RANGE_MINUTES / 5,
                        fiveMinBar.getHigh(), fiveMinBar.getLow(), fiveMinBar.getClose());

                // Check if we've collected enough bars for opening range
                if (fiveMinuteBars.size() >= OPENING_RANGE_MINUTES / 5) {
                    calculateOpeningRange();
                    transitionToBreakoutMonitoring();
                }
            }
        } catch (Exception e) {
            log.error("Error collecting opening range data", e);
        }
    }

    //    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorForBreakoutAndRetest() {
        if (currentState == TradingState.MONITORING_FOR_BREAKOUT ||
                currentState == TradingState.MONITORING_FOR_RETEST) {

            try {
                String date = LocalDate.now().toString();
                TwelveCandleBar oneMinBar = fetchOneMinuteCandle(SYMBOL, date);
                if (oneMinBar != null) {

                    if (currentState == TradingState.MONITORING_FOR_BREAKOUT) {
                        handleBreakoutMonitoring(oneMinBar);
                    } else if (currentState == TradingState.MONITORING_FOR_RETEST) {
                        handleRetestMonitoring(oneMinBar);
                    }

                    // Check timeouts
                    checkForTimeouts();
                }
            } catch (Exception e) {
                log.error("Error monitoring for breakout/retest", e);
            }
        }
    }

    private void initializeForNewTradingDay() {
        currentState = TradingState.COLLECTING_OPENING_RANGE;
        openingRange = null;
        breakoutData = null;
        fiveMinuteBars.clear();
        oneMinuteBreakoutBars.clear();
        marketOpenTime = LocalDateTime.now();
        breakoutStartTime = null;
        retestStartTime = null;

        log.info("Initialized for new trading day at {}", marketOpenTime);
    }

    private void calculateOpeningRange() {
        if (fiveMinuteBars.isEmpty()) {
            log.warn("No 5-minute bars available to calculate opening range");
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

        openingRange = new OpeningRange(high, low);
        log.info("Opening range calculated - High: {}, Low: {}, Range: {}",
                openingRange.high(), openingRange.low(),
                openingRange.high().subtract(openingRange.low()));
    }

    private void transitionToBreakoutMonitoring() {
        currentState = TradingState.MONITORING_FOR_BREAKOUT;
        breakoutStartTime = LocalDateTime.now();
        oneMinuteBreakoutBars.clear();

        log.info("Transitioning to breakout monitoring phase at {}", breakoutStartTime);
    }

    private void handleBreakoutMonitoring(TwelveCandleBar oneMinBar) {
        // Check for upward breakout
        if (new BigDecimal(oneMinBar.getClose()).compareTo(openingRange.high()) > 0) {
            oneMinuteBreakoutBars.add(oneMinBar);
            log.info("Potential breakout bar added: Close={} > Opening High={}",
                    oneMinBar.getClose(), openingRange.high());

            // Check if we have enough confirmation bars
            if (oneMinuteBreakoutBars.size() >= BREAKOUT_CONFIRMATION_BARS) {
                if (isBreakoutConfirmed()) {
                    confirmBreakout(oneMinBar);
                }
            }
        } else {
            // Reset if price closes back below opening high
            if (!oneMinuteBreakoutBars.isEmpty()) {
                log.info("Breakout attempt failed, resetting confirmation bars");
                oneMinuteBreakoutBars.clear();
            }
        }
    }

    private boolean isBreakoutConfirmed() {
        // All confirmation bars should close above opening high
        return oneMinuteBreakoutBars.stream()
                .allMatch(bar -> new BigDecimal(bar.getClose()).compareTo(openingRange.high()) > 0);
    }

    private void confirmBreakout(TwelveCandleBar breakoutBar) {
        breakoutData = new BreakoutData(
                new BigDecimal(breakoutBar.getClose()),
                LocalDateTime.now(),
                new BigDecimal(breakoutBar.getHigh())
        );

        currentState = TradingState.MONITORING_FOR_RETEST;
        retestStartTime = LocalDateTime.now();

        String logFileName = createLogFileName("OpeningBreakRange-", LocalDateTime.now().toString());

        // Write some sample lines to the log file
        writeToLog(logFileName, String.format("BREAKOUT %s with price - %s and high - %s", breakoutBar.getSymbol(),
                breakoutBar.getClose(), openingRange.high()));

        log.info("BREAKOUT CONFIRMED! Price: {}, Time: {}, Opening High: {}",
                breakoutData.breakoutPrice(), breakoutData.breakoutTime(), openingRange.high());
    }

    private void handleRetestMonitoring(TwelveCandleBar oneMinBar) {
        BigDecimal retestLevel = openingRange.high().add(RETEST_BUFFER);

        // Check if low of candle stays above retest level (successful retest)
        // OR if close drops below opening high (deeper retest)
        if (new BigDecimal(oneMinBar.getLow()).compareTo(retestLevel) >= 0) {
            // Shallow retest - price held above breakout level
            log.info("SHALLOW RETEST DETECTED - Low: {} held above retest level: {}",
                    oneMinBar.getLow(), retestLevel);
            confirmRetestAndPrepareEntry("SHALLOW");

        } else if (new BigDecimal(oneMinBar.getClose()).compareTo(openingRange.high()) <= 0) {
            // Deeper retest - price closed back below opening high
            log.info("DEEP RETEST DETECTED - Close: {} back below opening high: {}",
                    oneMinBar.getClose(), openingRange.high());
            confirmRetestAndPrepareEntry("DEEP");
        }
    }

    private void confirmRetestAndPrepareEntry(String retestType) {
        log.info("RETEST CONFIRMED ({})! Ready for entry logic.", retestType);

        // Calculate suggested entry parameters
        BigDecimal suggestedEntry = openingRange.high().add(new BigDecimal("0.01")); // Just above opening high
        BigDecimal suggestedStop = openingRange.low().subtract(new BigDecimal("0.01")); // Just below opening low
        BigDecimal riskAmount = suggestedEntry.subtract(suggestedStop);

        log.info("ENTRY SETUP - Suggested Entry: {}, Stop Loss: {}, Risk per share: {}",
                suggestedEntry, suggestedStop, riskAmount);


        // TODO: Implement your buy logic and risk management here
        // calculatePositionSize(riskAmount);
        // placeBuyOrder(suggestedEntry, suggestedStop);

        currentState = TradingState.SETUP_COMPLETE;
    }

    private void checkForTimeouts() {
        LocalDateTime now = LocalDateTime.now();

        if (currentState == TradingState.MONITORING_FOR_BREAKOUT && breakoutStartTime != null) {
            if (breakoutStartTime.plusMinutes(MAX_BREAKOUT_WAIT_MINUTES).isBefore(now)) {
                log.info("Breakout timeout reached, no valid breakout occurred within {} minutes",
                        MAX_BREAKOUT_WAIT_MINUTES);
                currentState = TradingState.TIMEOUT;
            }
        }

        if (currentState == TradingState.MONITORING_FOR_RETEST && retestStartTime != null) {
            if (retestStartTime.plusMinutes(MAX_RETEST_WAIT_MINUTES).isBefore(now)) {
                log.info("Retest timeout reached, no valid retest occurred within {} minutes",
                        MAX_RETEST_WAIT_MINUTES);
                currentState = TradingState.TIMEOUT;
            }
        }
    }

    // Mock methods - replace with your actual data fetching logic
    private TwelveCandleBar fetchFiveMinuteCandle(String symbol, String date) {
        // TODO: Implement your actual API call to fetch 5-minute candle
        // This is a mock implementation
        return twelveDataClient.quoteWithInterval(symbol, TimeFrame.FIVE_MIN, "date");
//        return createMockCandle();
    }

    private TwelveCandleBar fetchOneMinuteCandle(String symbol, String date) {
        // TODO: Implement your actual API call to fetch 1-minute candle
        // This is a mock implementation
//        return createMockCandle();
        return twelveDataClient.quoteWithInterval(symbol, TimeFrame.ONE_MIN, date);
    }

    private CandleBar createMockCandle() {
        // Mock data for testing - replace with actual implementation
        return new CandleBar(
                new BigDecimal("150.00"), // open
                new BigDecimal("151.00"), // high
                new BigDecimal("149.50"), // low
                new BigDecimal("150.75"), // close
                1000L, // volume
                LocalDateTime.now()
        );
    }


    public record CandleBar(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume,
                            LocalDateTime timestamp) {

    }

}
