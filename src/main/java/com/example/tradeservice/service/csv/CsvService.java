package com.example.tradeservice.service.csv;

import com.example.tradeservice.backtest.series.DoubleSeries;
import com.example.tradeservice.model.StockResponse;
import com.example.tradeservice.model.enums.TimeFrame;

import java.time.LocalDate;
import java.util.List;

public interface CsvService {

    void exportToCsvTwelve(String symbol, TimeFrame timeFrame,
                           List<StockResponse.Value> dataList);

    DoubleSeries initializeCsvForDay(String symbol, String date);

    void writeDayCsv(String symbol, String csv);

    DoubleSeries readDoubleSeries(String symbol, LocalDate from, LocalDate to);
}
