package com.example.tradeservice.handler;


import com.example.tradeservice.model.TradeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TradeEventListener {

    @EventListener
    public void handleOrderCompletedEvent(TradeUpdatedEvent event) {
        TradeData trade = event.getTrade();
        log.info("Trade updated: {} - ${} (volume: {}, time: {})",
                trade.getSymbol(), trade.getPrice(), trade.getVolume(), trade.getDateTime());
    }
}
