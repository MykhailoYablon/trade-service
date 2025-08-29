package com.example.tradeservice.strategy;

import com.example.tradeservice.strategy.enums.TradingState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.example.tradeservice.strategy.enums.TradingState.WAITING_FOR_MARKET_OPEN;

@Component
public class StrategyScheduler {

    @Autowired
    private Test service;
    private Map<String, TradingState> openingRangeLowHigh = Map.of("GOOG", WAITING_FOR_MARKET_OPEN,
            "MSFT", WAITING_FOR_MARKET_OPEN, "AMZN", WAITING_FOR_MARKET_OPEN);

    // Scheduled tasks
    @Scheduled(cron = "0 01-16/5 17 * * MON-FRI", zone = "GMT+3")
    public void batchOpeningRange() {
        openingRangeLowHigh.forEach(service::collectOpeningRangeData);
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void batchMonitor() {
        openingRangeLowHigh.forEach(service::monitorForBreakoutAndRetest);
    }

}
