package com.example.tradeservice.service;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

import java.math.BigDecimal;

public interface OrderTracker {
    void placeLimitOrder(Contract contract, String action, BigDecimal quantity, double price);

    void setOrder(Contract contract, Order order, OrderState orderState);

    void getAllOrders();
}
