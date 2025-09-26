package com.example.tradeservice.strategy;

import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.service.impl.PositionTracker;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Order;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@AllArgsConstructor
public class BuyAndHoldStrategy implements AsyncTradingStrategy {

    Map<String, Order> orders = new HashMap<>();

    private final OrderTracker orderTracker;
    private final PositionTracker positionTracker;

    @Override
    public CompletableFuture<TradingContext> startStrategy(TradingContext context) {
        return null;
    }

    @Override
    public CompletableFuture<List<Order>> onTick(TradingContext context) {
        return null;
    }
}
