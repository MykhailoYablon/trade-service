package com.example.tradeservice.strategy;

import com.example.tradeservice.model.PositionHolder;
import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.service.impl.PositionTracker;
import com.example.tradeservice.strategy.dataclient.StockDataClient;
import com.example.tradeservice.strategy.enums.TradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.Types;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class BuyAndHoldStrategy implements AsyncTradingStrategy {

    Map<String, Order> orders;

    private final StockDataClient dataClient;
    private final OrderTracker orderTracker;
    private final PositionTracker positionTracker;

    public BuyAndHoldStrategy(StockDataClient dataClient, OrderTracker orderTracker, PositionTracker positionTracker) {
        this.dataClient = dataClient;
        this.orderTracker = orderTracker;
        this.positionTracker = positionTracker;
    }

    @Override
    public CompletableFuture<TradingContext> startStrategy(TradingContext context) {
        context.getState().setCurrentState(TradingState.SETUP_COMPLETE);
        return CompletableFuture.completedFuture(context);
    }

    @Override
    public CompletableFuture<List<Order>> onTick(TradingContext context) {

        var symbol = context.getSymbol();
        var date = context.getDate();

        TwelveCandleBar oneMinBar = dataClient.quoteWithInterval(symbol, TimeFrame.ONE_MIN, date);

        BigDecimal suggestedStop = new BigDecimal(oneMinBar.getLow()).subtract(new BigDecimal("0.01"));

        Contract contract = Optional.ofNullable(positionTracker.getPositionBySymbol(symbol))
                .map(PositionHolder::getContract)
                .orElse(null);


        Order order = orderTracker.placeBuyAndHoldMarketOrder(contract, Types.Action.BUY, 1);

        return CompletableFuture.completedFuture(List.of(order));
    }
}
