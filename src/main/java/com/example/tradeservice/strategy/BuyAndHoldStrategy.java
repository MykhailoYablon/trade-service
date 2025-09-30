package com.example.tradeservice.strategy;

import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.strategy.enums.TradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class BuyAndHoldStrategy implements AsyncTradingStrategy {

    Map<String, Order> orders;

    private final OrderTracker orderTracker;

    public BuyAndHoldStrategy(OrderTracker orderTracker) {
        this.orderTracker = orderTracker;
    }

    @Override
    public CompletableFuture<TradingContext> startStrategy(TradingContext context) {
        context.getState().setCurrentState(TradingState.SETUP_COMPLETE);
        orders = null;
        return CompletableFuture.completedFuture(context);
    }

    @Override
    public CompletableFuture<List<Order>> onTick(TradingContext context) {

        if (Objects.isNull(orders)) {

            orders = new HashMap<>();
            String symbol = context.getSymbol();
            context.order(symbol, true, 100);

            Order order = orderTracker.placeBuyAndHoldMarketOrder(100);
            orders.put(symbol, order);
            return CompletableFuture.completedFuture(List.of(order));
        }
        return new CompletableFuture<>();
    }
}
