package com.example.tradeservice.service;

import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.TradeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeDataServiceTest {

    @Mock
    private RedisTemplate<String, TradeData> redisTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ValueOperations<String, TradeData> valueOperations;

    @Mock
    private ZSetOperations<String, TradeData> zSetOperations;

    @InjectMocks
    private TradeDataService tradeDataService;

    private TradeData mockTradeData;

    @BeforeEach
    void setUp() {
        mockTradeData = new TradeData();
        mockTradeData.setSymbol("AAPL");
        mockTradeData.setPrice(150.50);
        mockTradeData.setVolume(1000.0);
        mockTradeData.setTimestamp(Instant.now().toEpochMilli());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void processRealTimeTrade_ShouldStoreTradeAndPublishEvent() {
        // Given
        String symbol = "AAPL";
        String latestKey = "trade:latest:" + symbol;
        String historyKey = "trade:history:" + symbol;
        TradeData previousTrade = null;

        when(valueOperations.get(latestKey)).thenReturn(previousTrade);

        // When
        tradeDataService.processRealTimeTrade(mockTradeData);

        // Then
        verify(valueOperations).set(latestKey, mockTradeData, java.time.Duration.ofHours(24));
        verify(zSetOperations).add(historyKey, mockTradeData, mockTradeData.getTimestamp().doubleValue());
        verify(zSetOperations).removeRange(historyKey, 0, -1001);
        verify(redisTemplate).expire(historyKey, java.time.Duration.ofHours(24));

        ArgumentCaptor<TradeUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(TradeUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        TradeUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(mockTradeData, capturedEvent.getTrade());
        assertNull(capturedEvent.getPreviousTrade());
    }

    @Test
    void processRealTimeTrade_ShouldPublishEventWithPreviousTrade() {
        // Given
        String symbol = "AAPL";
        String latestKey = "trade:latest:" + symbol;
        String historyKey = "trade:history:" + symbol;
        
        TradeData previousTrade = new TradeData();
        previousTrade.setSymbol("AAPL");
        previousTrade.setPrice(150.00);
        previousTrade.setVolume(500.0);
        previousTrade.setTimestamp(Instant.now().minusSeconds(10).toEpochMilli());

        when(valueOperations.get(latestKey)).thenReturn(previousTrade);

        // When
        tradeDataService.processRealTimeTrade(mockTradeData);

        // Then
        ArgumentCaptor<TradeUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(TradeUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        TradeUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(mockTradeData, capturedEvent.getTrade());
        assertEquals(previousTrade, capturedEvent.getPreviousTrade());
    }

    @Test
    void getLatestTrade_ShouldReturnTrade_WhenTradeExists() {
        // Given
        String symbol = "AAPL";
        String key = "trade:latest:" + symbol;

        when(valueOperations.get(key)).thenReturn(mockTradeData);

        // When
        Optional<TradeData> result = tradeDataService.getLatestTrade(symbol);

        // Then
        assertTrue(result.isPresent());
        assertEquals(mockTradeData, result.get());
        verify(valueOperations).get(key);
    }

    @Test
    void getLatestTrade_ShouldReturnEmpty_WhenTradeDoesNotExist() {
        // Given
        String symbol = "NONEXISTENT";
        String key = "trade:latest:" + symbol;

        when(valueOperations.get(key)).thenReturn(null);

        // When
        Optional<TradeData> result = tradeDataService.getLatestTrade(symbol);

        // Then
        assertFalse(result.isPresent());
        verify(valueOperations).get(key);
    }

    @Test
    void getAllLatestTrades_ShouldReturnMapOfTrades() {
        // Given
        Set<String> keys = new HashSet<>(Arrays.asList("trade:latest:AAPL", "trade:latest:GOOGL"));
        List<TradeData> trades = Arrays.asList(mockTradeData, null);

        when(redisTemplate.keys("trade:latest:*")).thenReturn(keys);
        when(valueOperations.multiGet(keys)).thenReturn(trades);

        // When
        Map<String, TradeData> result = tradeDataService.getAllLatestTrades();

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey("AAPL"));
        assertEquals(mockTradeData, result.get("AAPL"));
        verify(redisTemplate).keys("trade:latest:*");
        verify(valueOperations).multiGet(keys);
    }

    @Test
    void getAllLatestTrades_ShouldReturnEmptyMap_WhenNoKeysFound() {
        // Given
        when(redisTemplate.keys("trade:latest:*")).thenReturn(null);

        // When
        Map<String, TradeData> result = tradeDataService.getAllLatestTrades();

        // Then
        assertTrue(result.isEmpty());
        verify(redisTemplate).keys("trade:latest:*");
        verify(valueOperations, never()).multiGet(any());
    }

    @Test
    void getLatestPrice_ShouldReturnPrice_WhenTradeExists() {
        // Given
        String symbol = "AAPL";
        when(valueOperations.get("trade:latest:" + symbol)).thenReturn(mockTradeData);

        // When
        Optional<Double> result = tradeDataService.getLatestPrice(symbol);

        // Then
        assertTrue(result.isPresent());
        assertEquals(150.50, result.get());
    }

    @Test
    void getLatestPrice_ShouldReturnEmpty_WhenTradeDoesNotExist() {
        // Given
        String symbol = "NONEXISTENT";
        when(valueOperations.get("trade:latest:" + symbol)).thenReturn(null);

        // When
        Optional<Double> result = tradeDataService.getLatestPrice(symbol);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getTradeHistory_ShouldReturnListOfTrades() {
        // Given
        String symbol = "AAPL";
        String historyKey = "trade:history:" + symbol;
        int limit = 10;
        
        Set<TradeData> trades = new HashSet<>(Arrays.asList(mockTradeData));
        when(zSetOperations.reverseRange(historyKey, 0, limit - 1)).thenReturn(trades);

        // When
        List<TradeData> result = tradeDataService.getTradeHistory(symbol, limit);

        // Then
        assertEquals(1, result.size());
        assertEquals(mockTradeData, result.get(0));
        verify(zSetOperations).reverseRange(historyKey, 0, limit - 1);
    }

    @Test
    void getTradeHistory_ShouldReturnEmptyList_WhenNoTradesFound() {
        // Given
        String symbol = "NONEXISTENT";
        String historyKey = "trade:history:" + symbol;
        int limit = 10;

        when(zSetOperations.reverseRange(historyKey, 0, limit - 1)).thenReturn(null);

        // When
        List<TradeData> result = tradeDataService.getTradeHistory(symbol, limit);

        // Then
        assertTrue(result.isEmpty());
        verify(zSetOperations).reverseRange(historyKey, 0, limit - 1);
    }

    @Test
    void getTradeHistoryBetween_ShouldReturnTradesInTimeRange() {
        // Given
        String symbol = "AAPL";
        String historyKey = "trade:history:" + symbol;
        long startTimestamp = Instant.now().minusSeconds(3600).toEpochMilli();
        long endTimestamp = Instant.now().toEpochMilli();
        
        Set<TradeData> trades = new HashSet<>(Arrays.asList(mockTradeData));
        when(zSetOperations.reverseRangeByScore(historyKey, startTimestamp, endTimestamp)).thenReturn(trades);

        // When
        List<TradeData> result = tradeDataService.getTradeHistoryBetween(symbol, startTimestamp, endTimestamp);

        // Then
        assertEquals(1, result.size());
        assertEquals(mockTradeData, result.get(0));
        verify(zSetOperations).reverseRangeByScore(historyKey, startTimestamp, endTimestamp);
    }

    @Test
    void clearTradeData_ShouldDeleteKeys() {
        // Given
        String symbol = "AAPL";
        String latestKey = "trade:latest:" + symbol;
        String historyKey = "trade:history:" + symbol;

        // When
        tradeDataService.clearTradeData(symbol);

        // Then
        verify(redisTemplate).delete(latestKey);
        verify(redisTemplate).delete(historyKey);
    }
}
