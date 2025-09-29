package com.example.tradeservice.service.csv;

import com.example.tradeservice.backtest.series.DoubleSeries;
import com.example.tradeservice.model.StockResponse;
import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CsvServiceImpl implements CsvService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_HEADER = "Stock Symbol,High,Low,Close,Date Time\n";
    private static final String DELIMITER = ";";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public void exportToCsvTwelve(String symbol, TimeFrame timeFrame,
                                  List<StockResponse.Value> dataList) {
        new File("exports/" + symbol + "/" + timeFrame).mkdirs();
        Map<String, List<StockResponse.Value>> grouped = dataList.stream()
                .collect(Collectors.groupingBy(obj -> {
                    // Extract just the date part (YYYY-MM-DD)
                    return obj.getDatetime().substring(0, 10);
                }));

        grouped.forEach((date, dayEntries) -> {
            String filePath = "exports/" + symbol + "/" + timeFrame + "/" + String.format("%s_data.csv", date);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                // Write header
                writer.print(CSV_HEADER);

                // Write data rows
                dayEntries.stream()
                        .sorted(Comparator.comparing(StockResponse.Value::getDatetime))
                        .forEach(data -> {
                            writer.printf("%s,%s,%s,%s,%s%n",
                                    escapeCsvValue(symbol),
                                    data.getHigh(),
                                    data.getLow(),
                                    data.getClose(),
                                    data.getDatetime()
                            );
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

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

    public void writeDayCsv(String symbol, String csv) {
        String filePath = "exports/" + symbol + "/day_data.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            csv.lines()
                    .map(line -> {
                        String[] parts = line.split(DELIMITER);
                        return parts[0] + ";" + parts[4];
                    })
                    .forEach(writer::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DoubleSeries readDoubleSeries(String symbol) {

        String fileName = "exports/" + symbol + "/day_data.csv";
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            Reader reader = new InputStreamReader(resource.getInputStream());

            CsvToBean<DoubleSeries> csvToBean = new CsvToBeanBuilder<DoubleSeries>(reader)
                    .withType(DoubleSeries.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<DoubleSeries> data = csvToBean.parse();

            reader.close();

            return data;
        } catch (Exception e) {
            log.info("No such file {}", fileName);
            return Collections.emptyList();
        }


    }

    public List<TwelveCandleBar> initializeBarsFromCsv(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            Reader reader = new InputStreamReader(resource.getInputStream());

            CsvToBean<TwelveCandleBar> csvToBean = new CsvToBeanBuilder<TwelveCandleBar>(reader)
                    .withType(TwelveCandleBar.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<TwelveCandleBar> data = csvToBean.parse();

            reader.close();

            return data;

        } catch (Exception e) {
            log.info("No such file {}", fileName);
            return Collections.emptyList();
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


    private String escapeCsvValue(String value) {
        if (value == null) return "";

        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
