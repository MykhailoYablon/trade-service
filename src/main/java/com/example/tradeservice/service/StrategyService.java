package com.example.tradeservice.service;

import com.example.tradeservice.configuration.FinnhubClient;
import com.example.tradeservice.configuration.TwelveDataClient;
import com.example.tradeservice.handler.StockTradeWebSocketHandler;
import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.TradeData;
import com.example.tradeservice.model.TwelveQuote;
import com.example.tradeservice.model.enums.TimeFrame;
import com.ib.client.EClientSocket;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Scope("singleton")
public class StrategyService {

    private final FinnhubClient finnhubClient;
    private final TwelveDataClient twelveDataClient;
    private final StockTradeWebSocketHandler handler;
    private final TradeDataService tradeDataService;
    private final RedisTemplate<String, TradeData> redisTemplate;

    private Map<String, Pair<Double, Double>> openingRangeLowHigh = new HashMap<>();
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RETRY_DELAY_SECONDS = 15;

    @Setter
    @NonNull
    private EClientSocket client;

    public StrategyService(FinnhubClient finnhubClient, TwelveDataClient twelveDataClient,
                           StockTradeWebSocketHandler handler, TradeDataService tradeDataService,
                           RedisTemplate<String, TradeData> redisTemplate) {
        this.finnhubClient = finnhubClient;
        this.twelveDataClient = twelveDataClient;
        this.handler = handler;
        this.tradeDataService = tradeDataService;
        this.redisTemplate = redisTemplate;
    }

    @EventListener
    public void handleOrderCompletedEvent(TradeUpdatedEvent event) {
        TradeData trade = event.getTrade();
        String symbol = trade.getSymbol();
        Double price = trade.getPrice();
        log.info("Trade updated: {} - ${} (volume: {}, time: {})",
                symbol, price, trade.getVolume(), trade.getDateTime());

        Pair<Double, Double> lowHigh = openingRangeLowHigh.get(symbol);
        var low = lowHigh.getFirst();
        var high = lowHigh.getSecond();

        // 4. Log breakout / create Order

        if (price > high) {
            log.info("BREAKOUT {} with price - {} and high - {}", symbol, price, high);

            String logFileName = createLogFileName();

            // Write some sample lines to the log file
            writeToLog(logFileName, String.format("BREAKOUT %s with price - %s and high - %s\"", symbol, price, high));

            //buy handler not implemented
            // add risk sell logic


            if (price <= low) {
                //sell logic
            }

            // 5. Unsubscribe from symbol if break happened
            handler.unsubscribeFromSymbol(symbol);
        } else {
            log.info("Price {} is lower than opening high {}", price, high);
        }

    }

    /**
     * Scheduled method that runs every day at 16:35 GMT+3
     * Cron expression: "0 35 16 * * ?"
     * - 0: seconds (0)
     * - 35: minutes (35)
     * - 16: hours (16 = 4:35 PM)
     * - *: day of month (every day)
     * - *: month (every month)
     * - ?: day of week (any day)
     */
    @Scheduled(cron = "0 35 16 * * MON-FRI", zone = "GMT+3")
    public void batch() {
        //batch not working due to free account limitation
        List<String> symbolList = List.of("GOOG");
        symbolList.forEach(this::openingRangeBreakStrategy);
    }

    public void openingRangeBreakStrategy(String symbol) {
        // 1. Fetch data for opening 15 min range asynchronously for several symbols
        //get last 5 min candle
        int attemptCount = 0;

        TwelveQuote quote = twelveDataClient.quoteWithInterval(symbol, TimeFrame.FIVE_MIN);

        while (attemptCount < MAX_RETRY_ATTEMPTS) {
            attemptCount++;
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            LocalDateTime dateTime = LocalDateTime.parse(quote.getDatetime(), dateTimeFormatter);
            LocalDate responseDate = dateTime.toLocalDate();
            LocalDate currentDate = LocalDate.now();

            if (responseDate.equals(currentDate)) {
                log.info("Date validation successful! Dates match.");
                log.info("Quote - {}", quote);
                // 2. Get opening 15 min range lows and highs for each symbol
                var high = Double.valueOf(quote.getHigh());
                var low = Double.valueOf(quote.getLow());

                openingRangeLowHigh.put(symbol, Pair.of(low, high));

                log.info("High - {}, Low - {}", high, low);

                // 3. Fetch async data in real time and set breakout function if new last_bar.close > opening_range_high
                handler.subscribeToSymbol(symbol);

                break;
            } else {
                log.warn("Date mismatch. Response date: {}, Current date: {}",
                        responseDate, currentDate);

                if (attemptCount < MAX_RETRY_ATTEMPTS) {
                    log.info("Retrying in {} seconds...", RETRY_DELAY_SECONDS);
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(RETRY_DELAY_SECONDS));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    // Make API call again to get fresh response
                    quote = twelveDataClient.quoteWithInterval(symbol, TimeFrame.FIVE_MIN);
                }
            }
        }
    }

    /**
     * Creates a log file name with current date
     * Format: OpeningBreakRange-YYYY-MM-DD.log
     */
    private static String createLogFileName() {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateString = currentDate.format(formatter);
        return "OpeningBreakRange-" + dateString + ".log";
    }

    /**
     * Writes a line to the log file
     *
     * @param fileName The log file name
     * @param message  The message to write
     */
    public static void writeToLog(String fileName, String message) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            // Add timestamp to each log entry
            String timestamp = LocalDateTime.now().toString();
            writer.write("[" + timestamp + "] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
}
