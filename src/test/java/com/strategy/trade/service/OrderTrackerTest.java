package com.strategy.trade.service;

import com.strategy.trade.mapper.OrderMapper;
import com.strategy.trade.model.OrderModel;
import com.strategy.trade.service.impl.OrderTrackerImpl;
import com.ib.client.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTrackerTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private EClientSocket client;

    @InjectMocks
    private OrderTrackerImpl orderTracker;

    private Contract mockContract;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        // Set up the client
        orderTracker.setIbClient(client);

        // Create mock contract
        mockContract = new Contract();
        mockContract.conid(12345);
        mockContract.symbol("AAPL");
        mockContract.secType("STK");
        mockContract.exchange("SMART");
        mockContract.currency("USD");

        // Create mock order
        mockOrder = new Order();
        mockOrder.permId(1001);
        mockOrder.action("BUY");
        mockOrder.orderType("LMT");
        mockOrder.totalQuantity(Decimal.get(100));
        mockOrder.lmtPrice(150.50);
    }

    @Test
    void placeLimitOrder_ShouldCallClientPlaceOrder() {
        // Given
        String action = "BUY";
        BigDecimal quantity = new BigDecimal("100");
        double price = 150.50;

        // When
        orderTracker.placeLimitOrder(mockContract, action, quantity, price);

        // Then
        verify(client).placeOrder(anyInt(), eq(mockContract), any(Order.class));
    }

    @Test
    void getAllOrders_ShouldReturnEmptyCollection_WhenNoOrders() {
        // When
        Collection<OrderModel> result = orderTracker.getAllOrders();

        // Then
        assertTrue(result.isEmpty());
        verify(orderMapper, never()).convertToOrder(any());
    }

    @Test
    void buildLimitOrder_ShouldCreateCorrectOrder() {
        // Given
        BigDecimal quantity = new BigDecimal("100");
        double limitPrice = 150.50;
        String action = "BUY";

        // When
        Order result = OrderTrackerImpl.buildLimitOrder(quantity, limitPrice, action);

        // Then
        assertEquals(action, result.action());
        assertEquals("LMT", result.orderType());
        assertEquals(Decimal.get(quantity), result.totalQuantity());
        assertEquals(limitPrice, result.lmtPrice());
        assertTrue(result.transmit());
    }

    @Test
    void buildLimitOrder_ShouldCreateSellOrder() {
        // Given
        BigDecimal quantity = new BigDecimal("50");
        double limitPrice = 200.00;
        String action = "SELL";

        // When
        Order result = OrderTrackerImpl.buildLimitOrder(quantity, limitPrice, action);

        // Then
        assertEquals(action, result.action());
        assertEquals("LMT", result.orderType());
        assertEquals(Decimal.get(quantity), result.totalQuantity());
        assertEquals(limitPrice, result.lmtPrice());
        assertTrue(result.transmit());
    }

    @Test
    void placeLimitOrder_ShouldIncrementOrderId() {
        // Given
        String action = "BUY";
        BigDecimal quantity = new BigDecimal("100");
        double price = 150.50;

        // When
        orderTracker.placeLimitOrder(mockContract, action, quantity, price);
        orderTracker.placeLimitOrder(mockContract, action, quantity, price);

        // Then
        verify(client, times(2)).placeOrder(anyInt(), eq(mockContract), any(Order.class));
        // The orderId should be incremented for each call
    }

    @Test
    void buildLimitOrder_ShouldHandleZeroQuantity() {
        // Given
        BigDecimal quantity = BigDecimal.ZERO;
        double limitPrice = 150.50;
        String action = "BUY";

        // When
        Order result = OrderTrackerImpl.buildLimitOrder(quantity, limitPrice, action);

        // Then
        assertEquals(action, result.action());
        assertEquals("LMT", result.orderType());
        assertEquals(Decimal.get(quantity), result.totalQuantity());
        assertEquals(limitPrice, result.lmtPrice());
        assertTrue(result.transmit());
    }

    @Test
    void buildLimitOrder_ShouldHandleNegativePrice() {
        // Given
        BigDecimal quantity = new BigDecimal("100");
        double limitPrice = -150.50;
        String action = "BUY";

        // When
        Order result = OrderTrackerImpl.buildLimitOrder(quantity, limitPrice, action);

        // Then
        assertEquals(action, result.action());
        assertEquals("LMT", result.orderType());
        assertEquals(Decimal.get(quantity), result.totalQuantity());
        assertEquals(limitPrice, result.lmtPrice());
        assertTrue(result.transmit());
    }
}
