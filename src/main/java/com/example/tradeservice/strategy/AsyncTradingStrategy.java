package com.example.tradeservice.strategy;

import com.example.tradeservice.strategy.enums.StrategyDataSource;
import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.enums.StrategyType;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Order;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncTradingStrategy {

    StrategyType getStrategyType();

    StrategyDataSource getStrategyDataSource();

    void setStrategyDataSource(StrategyDataSource strategyDataSource);

    CompletableFuture<TradingContext> startStrategy(TradingContext context);

    CompletableFuture<List<Order>> onTick(TradingContext context);

}
