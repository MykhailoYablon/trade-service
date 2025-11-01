package com.strategy.trade.strategy.model;

import com.strategy.trade.model.TwelveCandleBar;
import com.strategy.trade.strategy.enums.TradingState;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Helper class to manage per-symbol state
@Getter
public class SymbolTradingState {
    // Getters and setters
    @Setter
    private TradingState currentState = TradingState.WAITING_FOR_MARKET_OPEN;
    @Setter
    private OpeningRange openingRange;
    @Setter
    private BreakoutData breakoutData;
    private final List<TwelveCandleBar> fiveMinuteBars = new ArrayList<>();
    private final List<TwelveCandleBar> oneMinuteBreakoutBars = new ArrayList<>();
    @Setter
    private LocalDateTime marketOpenTime;
    @Setter
    private LocalDateTime breakoutStartTime;
    @Setter
    private LocalDateTime retestStartTime;

    @Setter
    private String testDate;

    public void reset() {
        currentState = TradingState.WAITING_FOR_MARKET_OPEN;
        openingRange = null;
        breakoutData = null;
        fiveMinuteBars.clear();
        oneMinuteBreakoutBars.clear();
        marketOpenTime = null;
        breakoutStartTime = null;
        retestStartTime = null;
        testDate = null;
    }

}
