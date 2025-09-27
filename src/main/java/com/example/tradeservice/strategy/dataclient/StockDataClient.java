package com.example.tradeservice.strategy.dataclient;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;

public interface StockDataClient {

    void initializeCsvForDay(String symbol, String date);

    TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame, String date);
}
