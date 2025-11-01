package com.strategy.trade.strategy;

import com.strategy.trade.service.OrderTracker;
import com.strategy.trade.strategy.enums.StrategyDataSource;
import com.strategy.trade.strategy.enums.StrategyType;
import com.strategy.trade.strategy.model.TradingContext;
import com.ib.client.Order;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class BuyAndHoldStrategy implements AsyncTradingStrategy {

    @Getter
    StrategyType strategyType = StrategyType.BUY_AND_HOLD;

    @Getter
    @Setter
    StrategyDataSource strategyDataSource = StrategyDataSource.CSV;

    Map<String, Order> orders;

    private final OrderTracker orderTracker;

    public BuyAndHoldStrategy(OrderTracker orderTracker) {
        this.orderTracker = orderTracker;
    }

    @Override
    public CompletableFuture<TradingContext> startStrategy(TradingContext context) {
        orders = null;
        return CompletableFuture.completedFuture(context);
    }

    @Override
    public CompletableFuture<List<Order>> onTick(TradingContext context) {

        if (Objects.isNull(orders)) {

            orders = new HashMap<>();
            String symbol = context.getSymbol();
            context.order(symbol, true, 100, BigDecimal.TEN);

            Order order = orderTracker.placeBuyAndHoldMarketOrder(100);
            orders.put(symbol, order);
            return CompletableFuture.completedFuture(List.of(order));
        }
        return new CompletableFuture<>();
    }
}
