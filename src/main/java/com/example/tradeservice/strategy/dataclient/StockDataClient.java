package com.example.tradeservice.strategy.dataclient;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;

public interface StockDataClient {

    TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame, String date);
}
