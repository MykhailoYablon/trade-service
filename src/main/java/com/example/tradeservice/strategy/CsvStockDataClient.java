package com.example.tradeservice.strategy;

import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class CsvStockDataClient implements StockDataClient {


    @Override
    public TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame) {


        return null;
    }
}
