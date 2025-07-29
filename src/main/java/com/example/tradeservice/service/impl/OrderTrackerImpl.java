package com.example.tradeservice.service.impl;

import com.example.tradeservice.mapper.OrderMapper;
import com.example.tradeservice.model.OrderHolder;
import com.example.tradeservice.model.OrderModel;
import com.example.tradeservice.service.OrderTracker;
import com.ib.client.*;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Scope("singleton")
public class OrderTrackerImpl implements OrderTracker {

    @Setter
    private int orderId = 100;

    private final OrderMapper orderMapper;

    private Map<Integer, OrderHolder> orders = new HashMap<>();

    @Setter
    @NonNull
    private EClientSocket client;

    public OrderTrackerImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public void placeLimitOrder(Contract contract, String action, BigDecimal quantity, double price) {
        Order order = buildLimitOrder(quantity, price, Types.Action.valueOf(action).getApiString());
        client.placeOrder(orderId++, contract, order);
    }

    @Override
    public void setOrder(Contract contract, Order order, OrderState orderState) {
        orders.put(order.permId(), new OrderHolder(order.permId(), order, contract, orderState));
    }

    @Override
    public Collection<OrderModel> getAllOrders() {
        return orders.values().stream()
                .map(orderMapper::convertToOrder)
                .toList();
    }

    public Collection<OrderHolder> getActiveOrdersByContract(Contract contract) {
        return orders.values().stream()
                .filter(orderHolder -> orderHolder.getContract().conid() == contract.conid())
                .filter(orderHolder -> orderHolder.getOrderState().status().isActive())
                .collect(Collectors.toList());
    }

    public void updateOrderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice) {
        OrderHolder orderHolder = orders.get(orderId);
        if(orderHolder != null) {
            orderHolder.getOrderState().status(status);
            orderHolder.getOrder().filledQuantity(filled);
        } else {
            throw new RuntimeException("Order empty for orderId=" + orderId);
        }
    }

    public static Order buildLimitOrder(BigDecimal quantity, double limitPrice, String action) {
        // ! [limitorder]
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.totalQuantity(Decimal.get(quantity));
        order.lmtPrice(limitPrice);
        //transmit order to IB
        order.transmit(true);
        // ! [limitorder]
        return order;
    }

}
