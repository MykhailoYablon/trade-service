package com.example.tradeservice.service.impl;

import com.example.tradeservice.mapper.OrderMapper;
import com.example.tradeservice.model.OrderHolder;
import com.example.tradeservice.model.OrderModel;
import com.example.tradeservice.service.OrderTracker;
import com.ib.client.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Scope("singleton")
public class OrderTrackerImpl implements OrderTracker {

    @Setter
    private int orderId = 100;

    private final OrderMapper orderMapper;

    private Map<Integer, OrderHolder> orders = new HashMap<>();

    @Value("${trading.default.stop-loss-range:2.0}")
    private double defaultStopLossRange;

    @Value("${trading.default.take-profit-range:3.0}")
    private double defaultTakeProfitRange;

    @Setter
    @NonNull
    private EClientSocket ibClient;

    public OrderTrackerImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public void placeLimitOrder(Contract contract, String action, BigDecimal quantity, double price) {
        Order order = buildLimitOrder(quantity, price, Types.Action.valueOf(action).getApiString());
        ibClient.placeOrder(orderId++, contract, order);
    }

    @Override
    public List<Order> placeMarketOrder(Contract contract, Types.Action action, double quantity, BigDecimal stopPrice) {
        int baseOrderId = ++orderId;
        List<Order> complexBracketOrder = createComplexBracketOrder(baseOrderId, stopPrice);
        // Ensure contract uses SMART routing
        contract.exchange("SMART");

        // Place orders with IB
        for (int i = 0; i < complexBracketOrder.size(); i++) {
            ibClient.placeOrder(baseOrderId + i, contract, complexBracketOrder.get(i));
        }

        log.info("Orders has been placed - {}", complexBracketOrder);
        return complexBracketOrder;
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
        if (orderHolder != null) {
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

    public List<Order> createComplexBracketOrder(int baseOrderId, BigDecimal stopPrice) {

        ComplexOrderConfig config = createSimpleConfig(Decimal.get(BigDecimal.TEN), Types.Action.BUY);

        List<Order> orders = new ArrayList<>();

        // 1. Create Parent Market Order
        Order parentOrder = createParentMarketOrder(config, baseOrderId);
        orders.add(parentOrder);

        // 2. Create Stop Loss Child Order
        Order stopLossOrder = createStopLossOrder(config, baseOrderId + 1, baseOrderId, stopPrice);
        orders.add(stopLossOrder);

        // 3. Create Take Profit Child Order
        Order takeProfitOrder = createTakeProfitOrder(config, baseOrderId + 2, baseOrderId, stopPrice.add(BigDecimal.valueOf(defaultTakeProfitRange)));
        orders.add(takeProfitOrder);

        return orders;
    }

    private Order createParentMarketOrder(ComplexOrderConfig config, int orderId) {
        Order order = new Order();

        // Basic order properties
        order.orderId(orderId);
        order.action(config.action());
        order.orderType(OrderType.MKT);
        order.totalQuantity(config.quantity());
//        order.tif(config.timeInForce());
//        order.outsideRth(config.outsideRegularTradingHours());

        // Enable bracket order transmission
        order.transmit(false); // Don't transmit parent until children are ready

        return order;
    }

    /**
     * Creates the stop loss child order
     */
    private Order createStopLossOrder(ComplexOrderConfig config, int orderId, int parentOrderId, BigDecimal stopPrice) {
        Order order = new Order();

        // Basic properties
        order.orderId(orderId);
        order.parentId(parentOrderId);
        order.action(getOppositeAction(config.action()));
        order.orderType(OrderType.STP);
        order.totalQuantity(config.quantity());
        order.tif(config.timeInForce());
        order.outsideRth(config.outsideRegularTradingHours());

        // Stop loss specific settings
        order.auxPrice(stopPrice.doubleValue()); // Will be set dynamically based on fill price and range

        // Advanced stop loss features
//        order.adjustedOrderType(OrderType.TRAIL_LIMIT);
//        order.adjustedStopPrice(200); // Trailing stop
//        order.adjustedStopLimitPrice(200); // Trailing stop limit

        order.transmit(false); // Don't transmit until take profit is ready

        return order;
    }

    /**
     * Creates the take profit child order
     */
    private Order createTakeProfitOrder(ComplexOrderConfig config, int orderId, int parentOrderId, BigDecimal profit) {
        Order order = new Order();

        // Basic properties
        order.orderId(orderId);
        order.parentId(parentOrderId);
        order.action(getOppositeAction(config.action()));
        order.orderType(OrderType.LMT);
        order.totalQuantity(config.quantity());
        order.tif(config.timeInForce());
        order.outsideRth(config.outsideRegularTradingHours());

        // Take profit settings
        order.lmtPrice(profit.doubleValue()); // Will be set based on fill price and range

        order.transmit(true); // Transmit all orders when this one is placed

        return order;
    }

    public ComplexOrderConfig createSimpleConfig(Decimal quantity, Types.Action action) {
        return new ComplexOrderConfig(
                quantity,
                defaultStopLossRange,
                defaultTakeProfitRange,
                action,
                "DAY",
                false
        );
    }

    private Types.Action getOppositeAction(Types.Action action) {
        return action == Types.Action.BUY ? Types.Action.SELL : Types.Action.BUY;
    }

    public record ComplexOrderConfig(Decimal quantity, double stopLossRange, double takeProfitRange, Types.Action action,
                                     String timeInForce, boolean outsideRegularTradingHours) {
    }

}
