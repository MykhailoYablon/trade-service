package com.example.tradeservice.strategy.enums;

public enum TradingState {
    WAITING_FOR_MARKET_OPEN,
    COLLECTING_OPENING_RANGE,
    MONITORING_FOR_BREAKOUT,
    MONITORING_FOR_RETEST,
    SETUP_COMPLETE,
    TIMEOUT
}
