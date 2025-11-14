package com.strategy.trade.configuration;

import com.strategy.trade.service.OrderTracker;
import com.strategy.trade.service.impl.PositionTracker;
import com.strategy.trade.strategy.AsyncOrbStrategy;
import com.strategy.trade.strategy.AsyncTradingStrategy;
import com.strategy.trade.strategy.dataclient.StockDataClient;
import com.strategy.trade.strategy.enums.StrategyDataSource;
import com.strategy.trade.strategy.enums.StrategyType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class TradingStrategyConfig {

    private final Map<StrategyType, AsyncTradingStrategy> strategies;

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

    // Spring will inject all beans implementing AsyncTradingStrategy
    public TradingStrategyConfig(List<AsyncTradingStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        AsyncTradingStrategy::getStrategyType,
                        Function.identity()
                ));
    }

    public AsyncTradingStrategy getStrategy(StrategyType type) {
        AsyncTradingStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for type: " + type);
        }
        return strategy;
    }
}
