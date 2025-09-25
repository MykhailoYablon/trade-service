package com.example.tradeservice.strategy.model;

import com.example.tradeservice.strategy.enums.StrategyMode;

public record TradingContext(String symbol, String date, SymbolTradingState state, StrategyMode mode) {
}
