package com.example.tradeservice.strategy;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.CsvToBeanFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class CsvStockDataClient implements StockDataClient {

    @Autowired
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void initializeCsvForDay(String symbol, String date) {
        //clear redis records
        String keyFiveMin = "candles:" + symbol + ":" + TimeFrame.FIVE_MIN + ":" + date;
        String keyOneMin = "candles:" + symbol + ":" + TimeFrame.ONE_MIN + ":" + date;
        redisTemplate.delete(keyFiveMin);
        redisTemplate.delete(keyOneMin);
        //rewrite to parse day
        String fiveMinFile = String.format("exports/%s/%s/%s_data.csv", symbol, TimeFrame.FIVE_MIN, date);

        String oneMinFile = String.format("exports/%s/%s/%s_data.csv", symbol, TimeFrame.ONE_MIN, date);

        //save 5 mins in redis
        List<TwelveCandleBar> fiveMinData = initializeBarsFromCsv(fiveMinFile);
        //save 1 min in redis
        List<TwelveCandleBar> oneMinData = initializeBarsFromCsv(oneMinFile);

        //save in Redis
        fiveMinData.forEach(record -> storeInRedis(record, TimeFrame.FIVE_MIN, date));

        oneMinData.forEach(record -> storeInRedis(record, TimeFrame.ONE_MIN, date));

        log.info("Initialized csv records for day - {}", date);

    }

    @Override
    public TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame, String date) {
        try {
            return fetchNextCandle(symbol, timeFrame, date);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<TwelveCandleBar> initializeBarsFromCsv(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            Reader reader = new InputStreamReader(resource.getInputStream());

            CsvToBean<TwelveCandleBar> csvToBean = new CsvToBeanBuilder<TwelveCandleBar>(reader)
                    .withType(TwelveCandleBar.class)
                    .withIgnoreLeadingWhiteSpace(true)
//                    .withFilter(getDateFilter(LocalDateTime.now().minusDays(10)))
                    .build();

            List<TwelveCandleBar> data = csvToBean.parse();

            reader.close();

            return data;

        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + fileName, e);
        }
    }

    private void storeInRedis(TwelveCandleBar candle, TimeFrame timeFrame, String date) {
        String key = "candles:" + candle.getSymbol() + ":" + timeFrame + ":" + date;
        String json;
        try {
            json = mapper.writeValueAsString(candle);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        redisTemplate.opsForList().rightPush(key, json); // enqueue at the end (FIFO)
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

//    private void storeInRedisTimeSeries(TwelveCandleBar candleBar) {
//        String timestampStr = candleBar.getDatetime();
//        String symbol = candleBar.getSymbol();
//        // Store in sorted set for time-based queries
//        redisTemplate.opsForZSet().add(
//                MINUTE_DATA_KEY + symbol,
//                timestampStr,
//                candleBar.getTimestamp()
//        );
//
//        // Store the actual candle data
//        Map<String, Object> candleData = new HashMap<>();
//        candleData.put("high", candleBar.getHigh());
//        candleData.put("low", candleBar.getLow());
//        candleData.put("close", candleBar.getClose());
//        candleData.put("symbol", candleBar.getSymbol());
//
//        redisTemplate.opsForHash().putAll(
//                MINUTE_DATA_KEY + symbol + ":" + timestampStr,
//                candleData
//        );
//    }
//
//    private TwelveCandleBar getFromRedisTimeSeries(String symbol, LocalDateTime timestamp, TimeFrame timeFrame) {
//        String timestampStr = timestamp.format(CSV_DATE_FORMAT);
//        String key = (timeFrame == TimeFrame.ONE_MIN ? MINUTE_DATA_KEY : FIVE_MIN_DATA_KEY) + symbol + ":" + timestampStr;
//
//        Map<Object, Object> candleData = redisTemplate.opsForHash().entries(key);
//
//        if (candleData.isEmpty()) {
//            return null;
//        }
//
//        try {
//            TwelveCandleBar twelveCandleBar = new TwelveCandleBar();
//            twelveCandleBar.setSymbol(candleData.get("symbol").toString());
//            twelveCandleBar.setHigh(candleData.get("high").toString());
//            twelveCandleBar.setLow(candleData.get("low").toString());
//            twelveCandleBar.setClose(candleData.get("close").toString());
//            twelveCandleBar.setDatetime(candleData.get("datetime").toString());
//            return twelveCandleBar;
//        } catch (Exception e) {
//            log.error("Error deserializing candle data for {} at {}", symbol, timestamp, e);
//            return null;
//        }
//    }
}
