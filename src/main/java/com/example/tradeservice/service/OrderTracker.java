package com.example.tradeservice.service;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.Types;

import java.math.BigDecimal;

public interface OrderTracker {
    void placeLimitOrder(Contract contract, Types.Action action, BigDecimal quantity, double price);

    void setOrder(Contract contract, Order order, OrderState orderState);
}
