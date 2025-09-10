package com.example.tradeservice.controller;

import com.example.tradeservice.configuration.FinnhubClient;
import com.example.tradeservice.handler.StockTradeWebSocketHandler;
import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.*;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.service.TradeDataService;
import com.example.tradeservice.service.impl.YearlyHistoricalDataService;
import com.example.tradeservice.strategy.CsvStockDataClient;
import com.example.tradeservice.strategy.RetestStrategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.example.tradeservice.service.impl.YearlyHistoricalDataService.isNonTradingDay;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/trades")
@AllArgsConstructor
public class TradeController {

    private final TradeDataService tradeDataService;
    private final StockTradeWebSocketHandler webSocketHandler;
    private final FinnhubClient finnhubClient;
    private final CsvStockDataClient dataClient;
    private final YearlyHistoricalDataService historicalDataService;
    private final RetestStrategy retestStrategy;

    // WebSocket subscription management
    @PostMapping("/subscribe/{symbol}")
    public ResponseEntity<Boolean> subscribeToSymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(webSocketHandler.subscribeToSymbol(symbol));
    }

    @PostMapping("/unsubscribe/{symbol}")
    public ResponseEntity<String> unsubscribeFromSymbol(@PathVariable String symbol) {
        webSocketHandler.unsubscribeFromSymbol(symbol);
        return ResponseEntity.ok("Unsubscribed from " + symbol);
    }

    // Latest trade data endpoints
    @GetMapping
    public ResponseEntity<TradeData> getLatestTrade(@RequestParam String symbol) {
        return tradeDataService.getLatestTrade(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{symbol}/price")
    public ResponseEntity<Double> getLatestPrice(@PathVariable String symbol) {
        return tradeDataService.getLatestPrice(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, TradeData>> getAllLatestTrades() {
        return ResponseEntity.ok(tradeDataService.getAllLatestTrades());
    }

    // Historical trade data endpoints
    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<TradeData>> getTradeHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit) {

        if (limit <= 0 || limit > 1000) {
            return ResponseEntity.badRequest().build();
        }

        List<TradeData> history = tradeDataService.getTradeHistory(symbol, limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{symbol}/history/range")
    public ResponseEntity<List<TradeData>> getTradeHistoryBetween(
            @PathVariable String symbol,
            @RequestParam long startTimestamp,
            @RequestParam long endTimestamp) {

        if (startTimestamp >= endTimestamp) {
            return ResponseEntity.badRequest().build();
        }

        List<TradeData> history = tradeDataService.getTradeHistoryBetween(symbol, startTimestamp, endTimestamp);
        return ResponseEntity.ok(history);
    }

    // Server-Sent Events endpoint for real-time updates to frontend
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TradeData>> streamTradeUpdates() {
        return Flux.create(emitter -> {
            ApplicationListener<TradeUpdatedEvent> listener = event -> {
                ServerSentEvent<TradeData> sse = ServerSentEvent.<TradeData>builder()
                        .id(event.getTrade().getSymbol())
                        .event("trade-update")
                        .data(event.getTrade())
                        .build();
                emitter.next(sse);
            };

            // Register listener and cleanup on disposal
            emitter.onDispose(() -> {
                // Remove listener when client disconnects
                log.info("Client disconnected from trade data stream");
            });
        });
    }

    @GetMapping("/status")
    public MarketStatus getMarketStatus() {
        return finnhubClient.marketStatus();
    }

    @GetMapping("/quote")
    public Quote quote(@RequestParam String symbol) {
        return finnhubClient.quote(symbol);
    }

    @GetMapping("/search")
    public SymbolLookup searchSymbol(@RequestParam String symbol) {
        return finnhubClient.search(symbol);
    }

    @GetMapping("/retest")
    public void retestDay(@RequestParam String symbol) throws InterruptedException {
// Define the year
        int year = 2025;

// Create formatter for the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

// Start from January 1st
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 9, 5);

// Process each month separately
        LocalDate currentMonth = startDate.withDayOfMonth(1);

        while (!currentMonth.isAfter(endDate)) {
            List<String> monthlyDateStrings = new ArrayList<>();

            // Get the last day of current month
            LocalDate lastDayOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth());
            LocalDate monthEndDate = lastDayOfMonth.isBefore(endDate) ? lastDayOfMonth : endDate;

            // Collect all trading days in this month
            LocalDate date = currentMonth;
            while (!date.isAfter(monthEndDate)) {
                if (!Boolean.TRUE.equals(isNonTradingDay(date))) {
                    monthlyDateStrings.add(date.format(formatter));
                }
                date = date.plusDays(1);
            }

            // Process this month's dates
            if (!monthlyDateStrings.isEmpty()) {
                log.info("Processing {} trading days for {}",
                        monthlyDateStrings.size(),
                        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

                List<CompletableFuture<Void>> monthlyFutures = monthlyDateStrings.stream()
                        .map(day -> retestStrategy.startStrategy(symbol, day))
                        .toList();

                // Wait for this month to complete before moving to next month
                LocalDate finalCurrentMonth = currentMonth;
                CompletableFuture.allOf(monthlyFutures.toArray(new CompletableFuture[0]))
                        .thenRun(() -> log.debug("Completed monitoring for {}",
                                finalCurrentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))))
                        .join(); // Wait for completion before proceeding to next month
            }

            // Move to next month
            currentMonth = currentMonth.plusMonths(1);
        }

    }

    @GetMapping("/csv")
    public void generateCsv(@RequestParam String symbol, @RequestParam TimeFrame timeFrame) {
        historicalDataService.collectYearlyDataEfficiently(symbol, timeFrame, 2025);
    }
}
