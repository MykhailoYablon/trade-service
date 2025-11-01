package com.strategy.trade.backtest;

import java.time.Instant;

public interface ClosedOrder extends Order {
    double getClosePrice();

    Instant getCloseInstant();

    default double getPl() {
        return calculatePl(getClosePrice());
    }
}
