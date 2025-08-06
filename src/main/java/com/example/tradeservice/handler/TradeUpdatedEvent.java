package com.example.tradeservice.handler;

import com.example.tradeservice.model.TradeData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TradeUpdatedEvent extends ApplicationEvent {
    private final TradeData trade;
    private final TradeData previousTrade;

    public TradeUpdatedEvent(Object source, TradeData trade, TradeData previousTrade) {
        super(source);
        this.trade = trade;
        this.previousTrade = previousTrade;
    }

}
