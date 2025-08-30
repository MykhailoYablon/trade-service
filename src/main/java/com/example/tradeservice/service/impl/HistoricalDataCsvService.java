package com.example.tradeservice.service.impl;

import com.example.tradeservice.entity.HistoricalData;
import com.example.tradeservice.model.StockResponse;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HistoricalDataCsvService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_HEADER = "Stock Symbol,Timeframe,High,Low,Close,Date Time\n";

    public void exportToCsv(HistoricalData data, String filePath) throws IOException {

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Write header
            writer.print(CSV_HEADER);

            // Write data rows
//            for (HistoricalData data : historicalDataList) {
            writer.printf("%s,%s,%s,%s,%s,%s%n",
                    escapeCsvValue(data.getPosition().getSymbol()),
                    data.getTimeframe().toString(),
                    data.getHigh().toString(),
                    data.getLow().toString(),
                    data.getClose().toString(),
                    data.getTimestamp().format(DATE_FORMATTER)
            );
        }
//        }
    }

    public void exportToCsvTwelve(String symbol, String timeframe, List<StockResponse.Value> dataList, String filePath) throws IOException {

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Write header
            writer.print(CSV_HEADER);

            // Write data rows
            for (StockResponse.Value data : dataList) {
                writer.printf("%s,%s,%s,%s,%s,%s%n",
                        escapeCsvValue(symbol),
                        timeframe,
                        data.getHigh(),
                        data.getLow(),
                        data.getClose(),
                        data.getDatetime()
                );
            }
        }
    }

    public String generateCsvContent(List<HistoricalData> historicalDataList) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER);

        for (HistoricalData data : historicalDataList) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s%n",
                    escapeCsvValue(data.getPosition().getSymbol()),
                    data.getTimeframe().toString(),
                    data.getHigh().toString(),
                    data.getLow().toString(),
                    data.getClose().toString(),
                    data.getTimestamp().format(DATE_FORMATTER)
            ));
        }

        return csv.toString();
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
