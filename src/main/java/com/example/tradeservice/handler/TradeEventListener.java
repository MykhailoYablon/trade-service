package com.example.tradeservice.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TradeEventListener {

    @EventListener
    public void handleOrderCompletedEvent(TradeUpdatedEvent event) {
//        System.out.println("Sending notification for order: " + event.getOrderId() + " to customer: " + event.getCustomerId());
        // Logic to send email, push notification, etc.
    }
}
