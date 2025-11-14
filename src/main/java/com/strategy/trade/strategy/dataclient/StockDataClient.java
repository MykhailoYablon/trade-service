package com.strategy.trade.strategy.dataclient;

import com.strategy.trade.model.TwelveCandleBar;
import com.strategy.trade.model.enums.TimeFrame;

public interface StockDataClient {

    TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame, String date);

}
