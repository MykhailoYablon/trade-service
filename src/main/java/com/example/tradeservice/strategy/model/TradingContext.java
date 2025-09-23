package com.example.tradeservice.strategy.model;

public record TradingContext(String symbol, String date, SymbolTradingState state) {
}
