package com.example.tradeservice.backtest;


import java.time.Instant;

public class SimpleClosedOrder implements ClosedOrder {
    SimpleOrder order;
    double closePrice;
    Instant closeInstant;
    double pl;

    public SimpleClosedOrder(SimpleOrder order, double closePrice, Instant closeInstant) {
        this.order = order;
        this.closePrice = closePrice;
        this.closeInstant = closeInstant;
        pl = calculatePl(this.closePrice);
    }

    @Override public int getId() {
        return order.getId();
    }

    @Override public double getClosePrice() {
        return closePrice;
    }

    @Override public Instant getCloseInstant() {
        return closeInstant;
    }

    @Override public double getPl() {
        return pl;
    }

    @Override public boolean isLong() {
        return order.isLong();
    }

    @Override public int getAmount() {
        return order.getAmount();
    }

    @Override public double getOpenPrice() {
        return order.getOpenPrice();
    }

    @Override public Instant getOpenInstant() {
        return order.getOpenInstant();
    }

    @Override public String getInstrument() {
        return order.getInstrument();
    }
}
