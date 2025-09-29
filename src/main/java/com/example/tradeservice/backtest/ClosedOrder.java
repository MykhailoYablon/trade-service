package com.example.tradeservice.backtest;

import java.time.Instant;

public interface ClosedOrder extends Order {
    double getClosePrice();

    Instant getCloseInstant();

    default double getPl() {
        return calculatePl(getClosePrice());
    }
}
