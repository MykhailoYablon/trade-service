package com.strategy.trade.backtest;


import java.time.Instant;

public class SimpleOrder implements Order {
    int id;
    int amount;
    double openPrice;
    Instant openInstant;
    String instrument;

    public SimpleOrder(int id, String instrument, Instant openInstant, double openPrice, int amount) {
        this.id = id;
        this.instrument = instrument;
        this.openInstant = openInstant;
        this.openPrice = openPrice;
        this.amount = amount;
    }

    @Override public int getId() {
        return id;
    }

    @Override public int getAmount() {
        return amount;
    }

    @Override public double getOpenPrice() {
        return openPrice;
    }

    @Override public Instant getOpenInstant() {
        return openInstant;
    }

    @Override public String getInstrument() {
        return instrument;
    }
}
