package com.example.tradeservice.service.csv;

import com.example.tradeservice.backtest.series.DoubleSeries;

public interface CsvService {

    DoubleSeries initializeCsvForDay(String symbol, String date);
}
