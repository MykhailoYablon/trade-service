package com.example.tradeservice.service;

import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.Quote;
import com.example.tradeservice.model.TradeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TradeDataService {

    private final Map<String, TradeData> latestTrades = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;

    public TradeDataService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void processRealTimeTrade(TradeData trade) {
        String symbol = trade.getSymbol();
        TradeData previousTrade = latestTrades.put(symbol, trade);

        log.debug("Updated trade for {}: ${} (volume: {})", symbol, trade.getPrice(), trade.getVolume());

        // Publish event for other components to react to
        eventPublisher.publishEvent(new TradeUpdatedEvent(this, trade, previousTrade));
    }

    public Optional<TradeData> getLatestTrade(String symbol) {
        return Optional.ofNullable(latestTrades.get(symbol));
    }

    public Map<String, TradeData> getAllLatestTrades() {
        return new HashMap<>(latestTrades);
    }

    public Optional<Double> getLatestPrice(String symbol) {
        return getLatestTrade(symbol).map(TradeData::getPrice);
    }

    @EventListener
    public void handleTradeUpdate(TradeUpdatedEvent event) {
        // Handle trade updates (e.g., send to UI, trigger alerts, etc.)
        TradeData trade = event.getTrade();
        log.info("Trade updated: {} - ${} (volume: {}, time: {})",
                trade.getSymbol(), trade.getPrice(), trade.getVolume(), trade.getDateTime());
    }
}
