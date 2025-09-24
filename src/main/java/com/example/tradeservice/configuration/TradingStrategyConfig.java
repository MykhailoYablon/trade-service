package com.example.tradeservice.configuration;

import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.service.impl.PositionTracker;
import com.example.tradeservice.strategy.AsyncOrbStrategy;
import com.example.tradeservice.strategy.AsyncTradingStrategy;
import com.example.tradeservice.strategy.dataclient.StockDataClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TradingStrategyConfig {

    @Bean
    @Qualifier("twelveDataStrategy")
    public AsyncTradingStrategy twelveDataStrategy(@Qualifier("twelveData") StockDataClient twelveDataClient,
                                                   OrderTracker orderTracker,
                                                   PositionTracker positionTracker) {
        return new AsyncOrbStrategy(twelveDataClient, orderTracker, positionTracker);
    }

    @Bean
    @Qualifier("csvDataStrategy")
    public AsyncTradingStrategy csvDataStrategy(@Qualifier("csvData") StockDataClient csvDataClient,
                                                   OrderTracker orderTracker,
                                                   PositionTracker positionTracker) {
        return new AsyncOrbStrategy(csvDataClient, orderTracker, positionTracker);
    }
}
