package com.strategy.trade.service.csv;

import com.strategy.trade.backtest.series.DoubleSeries;
import com.strategy.trade.model.StockResponse;
import com.strategy.trade.model.enums.TimeFrame;

import java.time.LocalDate;
import java.util.List;

public interface CsvService {

    void exportToCsvTwelve(String symbol, TimeFrame timeFrame,
                           List<StockResponse.Value> dataList);

    DoubleSeries initializeCsvForDay(String symbol, String date);

    void writeDayCsv(String symbol, String csv);

    DoubleSeries readDoubleSeries(String symbol, LocalDate from, LocalDate to);
}
