package com.example.tradeservice.handler;


import com.example.tradeservice.model.TradeData;
import com.example.tradeservice.service.StrategyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class TradeEventListener {

    private final StrategyService strategyService;
    private final StockTradeWebSocketHandler handler;

    @EventListener
    public void handleOrderCompletedEvent(TradeUpdatedEvent event) {
        TradeData trade = event.getTrade();
        log.info("Trade updated: {} - ${} (volume: {}, time: {})",
                trade.getSymbol(), trade.getPrice(), trade.getVolume(), trade.getDateTime());

        strategyService.openingRangeBreakStrategy();



        String symbol = event.getTrade().getSymbol();

        handler.unsubscribeFromSymbol(symbol);
    }
}
