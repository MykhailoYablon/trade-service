package com.strategy.trade.strategy.dataclient;

import com.strategy.trade.model.TwelveCandleBar;
import com.strategy.trade.model.enums.TimeFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBeanFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component("csvData")
@Qualifier("csvData")
@Slf4j
@AllArgsConstructor
public class CsvStockDataClient implements StockDataClient {

    @Autowired
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame, String date) {
        try {
            return fetchNextCandle(symbol, timeFrame, date);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TwelveCandleBar fetchNextCandle(String symbol, TimeFrame timeFrame, String date) throws Exception {
        String key = "candles:" + symbol + ":" + timeFrame + ":" + date;
        String json = redisTemplate.opsForList().leftPop(key); // dequeue oldest
        if (json == null) return null;
        return mapper.readValue(json, TwelveCandleBar.class);
    }

    private CsvToBeanFilter getDateFilter(LocalDateTime dateTime) {
        return line -> {
            // Assuming datetime is in the second column (index 1) and format "yyyy-MM-dd HH:mm:ss"
            String datetimeString = line[5];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            try {
                LocalDateTime currentEventTime = LocalDateTime.parse(datetimeString, formatter);
                return !currentEventTime.isBefore(dateTime) && currentEventTime.isBefore(dateTime.plusDays(1));
            } catch (Exception e) {
                // Handle parsing errors, e.g., log them or skip the line
                return false;
            }
        };
    }
}
