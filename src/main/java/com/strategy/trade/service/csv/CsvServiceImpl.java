package com.strategy.trade.service.csv;

import com.strategy.trade.backtest.series.DoubleSeries;
import com.strategy.trade.backtest.series.TimeSeries;
import com.strategy.trade.model.StockResponse;
import com.strategy.trade.model.TwelveCandleBar;
import com.strategy.trade.model.enums.TimeFrame;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CsvServiceImpl implements CsvService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String CSV_HEADER = "Stock Symbol,High,Low,Close,Date Time\n";
    private static final String DELIMITER = ";";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
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
    public DoubleSeries initializeCsvForDay(String symbol, String date) {
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


        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        oneMinData.stream()
                        .map(candle -> {
                            // Parse datetime
                            LocalDate candleDate = LocalDate.parse(candle.getDatetime(), DATE_FORMATTER);
                            Instant instant = candleDate.atStartOfDay(ZoneOffset.UTC).toInstant();

                            // Parse close value
                            Double closeValue = Double.parseDouble(candle.getClose());

                            // Create entry
                            return new TimeSeries.Entry<>(closeValue, instant);
                        })
                                .forEach(entries::add);

        log.info("Initialized csv records for day - {}", date);
        return new DoubleSeries(entries, symbol).toAscending();

    }

    @Override
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

    @Override
    public DoubleSeries readDoubleSeries(String symbol, LocalDate from, LocalDate to) {
        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        String filePath = "exports/" + symbol + "/day_data.csv";

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .build();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(parser)
                .build()) {
            List<String[]> rows = reader.readAll();

            // Skip header row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                // Parse datetime
                LocalDate date = LocalDate.parse(row[0], DAY_FORMATTER);

                // Apply date range filter
                if (from != null && date.isBefore(from)) {
                    continue;
                }
                if (to != null && date.isAfter(to)) {
                    continue;
                }

                // Parse datetime
                Instant instant = date.atStartOfDay(ZoneOffset.UTC).toInstant();

                // Parse close value
                Double closeValue = Double.parseDouble(row[1]);

                // Create entry
                entries.add(new TimeSeries.Entry<>(closeValue, instant));
            }
        } catch (IOException | CsvException ex) {
            log.info("There is no csv initialized yet");
            throw new RuntimeException(ex);
        }

        return new DoubleSeries(entries, symbol).toAscending();

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
