package com.example.tradeservice.strategy;

import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Order;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AsyncTradingStrategy {

    CompletableFuture<TradingContext> startStrategy(TradingContext context);

    CompletableFuture<List<Order>> onTick(TradingContext context);

}
