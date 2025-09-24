package com.example.tradeservice.strategy;

import com.example.tradeservice.strategy.model.TradingContext;

import java.util.concurrent.CompletableFuture;

public interface AsyncTradingStrategy {

    CompletableFuture<Void> startStrategy(TradingContext context);

    CompletableFuture<Void> onTick();

    boolean shouldMonitorSymbol();

}
