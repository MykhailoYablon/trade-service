package com.example.tradeservice.service.csv;

import com.example.tradeservice.model.StockResponse;
import com.example.tradeservice.model.enums.TimeFrame;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CsvService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_HEADER = "Stock Symbol,High,Low,Close,Date Time\n";

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

    private String escapeCsvValue(String value) {
        if (value == null) return "";

        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
