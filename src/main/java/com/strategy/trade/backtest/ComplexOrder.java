package com.strategy.trade.backtest;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;

@Getter
@Setter
public class ComplexOrder extends SimpleOrder {

    double stopLossPrice;
    double takeProfitPrice;

    @Value("${trading.default.stop-loss-range:2.0}")
    private double defaultStopLossRange;

    @Value("${trading.default.take-profit-range:3.0}")
    private double defaultTakeProfitRange;

    public ComplexOrder(int id, String instrument, Instant openInstant, double openPrice, int amount) {
        super(id, instrument, openInstant, openPrice, amount);
        this.stopLossPrice = openPrice + defaultStopLossRange;
        this.takeProfitPrice = openPrice + defaultTakeProfitRange;
    }
}
