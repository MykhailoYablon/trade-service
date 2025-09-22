package com.example.tradeservice.service;

import com.example.tradeservice.model.OrderModel;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.Types;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface OrderTracker {
    void placeLimitOrder(Contract contract, String action, BigDecimal quantity, double price);

    List<Order> placeMarketOrder(Contract contract, Types.Action action, double quantity, BigDecimal stopPrice);

    void setOrder(Contract contract, Order order, OrderState orderState);

    Collection<OrderModel> getAllOrders();
}
