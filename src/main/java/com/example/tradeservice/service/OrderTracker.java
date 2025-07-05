package com.example.tradeservice.service;

import com.example.tradeservice.model.OrderHolder;
import com.ib.client.Decimal;
import com.ib.client.EClientSocket;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Scope("singleton")
public class OrderTracker {

    @Setter
    private int orderId = 100;

    private Map<Integer, OrderHolder> orders = new HashMap<>();

    @Setter
    @NonNull
    private EClientSocket client;

    public void updateOrderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice) {
        OrderHolder orderHolder = orders.get(orderId);
        if(orderHolder != null) {
            orderHolder.getOrderState().status(status);
            orderHolder.getOrder().filledQuantity(filled);
        } else {
            throw new RuntimeException("Order empty for orderId=" + orderId);
        }
    }


}
