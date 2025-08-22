package com.example.tradeservice.service;

import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.TradeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TradeDataService {

    private static final String LATEST_TRADE_KEY_PREFIX = "trade:latest:";
    private static final String TRADE_HISTORY_KEY_PREFIX = "trade:history:";
    private static final Duration TRADE_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, TradeData> redisTemplate;

    private final Map<String, TradeData> latestTrades = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;

    public TradeDataService(RedisTemplate<String, TradeData> redisTemplate, ApplicationEventPublisher eventPublisher) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    public void processRealTimeTrade(TradeData trade) {
        String symbol = trade.getSymbol();
        String latestKey = LATEST_TRADE_KEY_PREFIX + symbol;

        // Get previous trade for event
        TradeData previousTrade = redisTemplate.opsForValue().get(latestKey);

        // Store latest trade with TTL
        redisTemplate.opsForValue().set(latestKey, trade, TRADE_TTL);

        // Store in time series (sorted set with timestamp as score)
        String historyKey = TRADE_HISTORY_KEY_PREFIX + symbol;
        redisTemplate.opsForZSet().add(historyKey, trade, trade.getTimestamp().doubleValue());

        // Keep only last 1000 trades per symbol
        redisTemplate.opsForZSet().removeRange(historyKey, 0, -1001);
        redisTemplate.expire(historyKey, TRADE_TTL);

        log.debug("Stored trade for {}: ${} (volume: {})", symbol, trade.getPrice(), trade.getVolume());

        // Publish event for other components to react to
        eventPublisher.publishEvent(new TradeUpdatedEvent(this, trade, previousTrade));

    }

    public Optional<TradeData> getLatestTrade(String symbol) {
        String key = LATEST_TRADE_KEY_PREFIX + symbol;
        TradeData trade = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(trade);
    }

    public Map<String, TradeData> getAllLatestTrades() {
        String pattern = LATEST_TRADE_KEY_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        List<TradeData> trades = redisTemplate.opsForValue().multiGet(keys);
        Map<String, TradeData> result = new HashMap<>();

        int index = 0;
        for (String key : keys) {
            if (trades != null && index < trades.size() && trades.get(index) != null) {
                String symbol = key.substring(LATEST_TRADE_KEY_PREFIX.length());
                result.put(symbol, trades.get(index));
            }
            index++;
        }

        return result;
    }

    public Optional<Double> getLatestPrice(String symbol) {
        return getLatestTrade(symbol).map(TradeData::getPrice);
    }

    //rewrite to fetch last n minutes
    public List<TradeData> getTradeHistory(String symbol, int minutes) {
        String historyKey = TRADE_HISTORY_KEY_PREFIX + symbol;
        Set<TradeData> trades = redisTemplate.opsForZSet()
                .reverseRange(historyKey, 0, minutes - 1);

        return trades != null ? new ArrayList<>(trades) : new ArrayList<>();
    }

    public List<TradeData> getTradeHistoryBetween(String symbol, long startTimestamp, long endTimestamp) {
        String historyKey = TRADE_HISTORY_KEY_PREFIX + symbol;
        Set<TradeData> trades = redisTemplate.opsForZSet()
                .reverseRangeByScore(historyKey, startTimestamp, endTimestamp);

        return trades != null ? new ArrayList<>(trades) : new ArrayList<>();
    }

    public void clearTradeData(String symbol) {
        String latestKey = LATEST_TRADE_KEY_PREFIX + symbol;
        String historyKey = TRADE_HISTORY_KEY_PREFIX + symbol;

        redisTemplate.delete(latestKey);
        redisTemplate.delete(historyKey);

        log.info("Cleared trade data for symbol: {}", symbol);
    }

}
