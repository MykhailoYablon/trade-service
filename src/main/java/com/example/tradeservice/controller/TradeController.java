package com.example.tradeservice.controller;

import com.example.tradeservice.configuration.FinnhubClient;
import com.example.tradeservice.handler.StockTradeWebSocketHandler;
import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.MarketStatus;
import com.example.tradeservice.model.TradeData;
import com.example.tradeservice.service.TradeDataService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/trades")
@AllArgsConstructor
public class TradeController {

    private final TradeDataService tradeDataService;
    private final StockTradeWebSocketHandler webSocketHandler;
    private final FinnhubClient finnhubClient;

    @PostMapping("/subscribe/{symbol}")
    public ResponseEntity<String> subscribeToSymbol(@PathVariable String symbol) {
        webSocketHandler.subscribeToSymbol(symbol);
        return ResponseEntity.ok("Subscribed to " + symbol);
    }

    @PostMapping("/unsubscribe/{symbol}")
    public ResponseEntity<String> unsubscribeFromSymbol(@PathVariable String symbol) {
        webSocketHandler.unsubscribeFromSymbol(symbol);
        return ResponseEntity.ok("Unsubscribed from " + symbol);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<TradeData> getLatestTrade(@PathVariable String symbol) {
        return tradeDataService.getLatestTrade(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, TradeData>> getAllLatestTrades() {
        return ResponseEntity.ok(tradeDataService.getAllLatestTrades());
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
}
