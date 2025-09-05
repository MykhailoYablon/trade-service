package com.example.tradeservice.controller;

import com.example.tradeservice.configuration.FinnhubClient;
import com.example.tradeservice.handler.StockTradeWebSocketHandler;
import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.*;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.service.TradeDataService;
import com.example.tradeservice.service.impl.HistoricalDataCsvService;
import com.example.tradeservice.service.impl.YearlyHistoricalDataService;
import com.example.tradeservice.strategy.CsvStockDataClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private final HistoricalDataCsvService csvService;
    private final YearlyHistoricalDataService historicalDataService;

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
    public List<StockResponse.Value> retestToday(@RequestParam String symbol, @RequestParam String date) {

        if (Boolean.TRUE.equals(isNonTradingDay(LocalDate.parse(date)))) {
            return Collections.emptyList();
        }

        dataClient.initializeCsvForDay(symbol, date);

        TwelveCandleBar twelveCandleBar = dataClient.quoteWithInterval(symbol, TimeFrame.FIVE_MIN);


        return null;
    }

    @GetMapping("/csv")
    public void generateCsv(@RequestParam String symbol, @RequestParam TimeFrame timeFrame) {
        List<StockResponse.Value> candles;
        new File("exports/" + symbol + "/" + timeFrame).mkdirs();
        if (TimeFrame.FIVE_MIN.equals(timeFrame)) {
            candles = historicalDataService.collectYearlyDataEfficiently(symbol, TimeFrame.FIVE_MIN, 2025, 3);
        } else {
            candles = historicalDataService
                    .collectYearlyDataEfficiently(symbol, TimeFrame.ONE_MIN, 2025, null);
        }

        csvService.exportToCsvTwelve(symbol, timeFrame, candles);
    }
}
