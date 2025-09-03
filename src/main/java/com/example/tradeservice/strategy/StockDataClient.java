package com.example.tradeservice.strategy;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;

public interface StockDataClient {

    void initializeCsvForDay(String symbol);

    TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame);
}
