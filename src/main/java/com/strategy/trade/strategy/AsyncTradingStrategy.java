package com.strategy.trade.strategy;

import com.strategy.trade.strategy.enums.StrategyDataSource;
import com.strategy.trade.strategy.enums.StrategyType;
import com.strategy.trade.strategy.model.TradingContext;
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
