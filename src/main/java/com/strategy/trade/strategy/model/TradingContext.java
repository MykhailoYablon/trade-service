package com.strategy.trade.strategy.model;

import com.example.tradeservice.backtest.*;
import com.strategy.trade.backtest.*;
import com.strategy.trade.backtest.series.DoubleSeries;
import com.strategy.trade.strategy.enums.StrategyMode;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class TradingContext {
    Instant instant;
    Double currentPrice;
    List<String> instruments;

    public DoubleSeries profitLoss = new DoubleSeries("pl");
    public DoubleSeries fundsHistory = new DoubleSeries("funds");
    public DoubleSeries mHistory;

    private String symbol;
    private String date;
    private SymbolTradingState state;
    private StrategyMode mode;
    @Value("${trading.default.stop-loss-range:2.0}")
    private double defaultStopLossRange;

    @Value("${trading.default.take-profit-range:3.0}")
    private double defaultTakeProfitRange;


    double initialFunds;
    double commissions;
    int orderId = 1;
    double leverage;
    double closedPl = 0;
    //    List<SimpleClosedOrder> mClosedOrders = new ArrayList<>();
    List<ClosedOrder> closedOrders = new ArrayList<>();


    List<Order> orders = new ArrayList<>();
    List<ComplexOrder> complexOrders = new ArrayList<>();

    public double getPL() {
        return closedPl + orders.stream().mapToDouble(o -> o.calculatePl(getLastPrice()))
                .sum() - commissions;
    }

    public double onTickPL() {
        if (complexOrders.isEmpty()) {
            return 0;
        }
        double totalPL = 0.0;
        ComplexOrder order = complexOrders.getFirst();
        boolean shouldClose = false;
        double exitPrice = currentPrice;
        double entryPrice = order.getOpenPrice();

        // Check stop loss
        if (order.getStopLossPrice() != 0) {
            double stopPrice = order.getStopLossPrice();
            if (currentPrice <= stopPrice) {
                shouldClose = true;
                exitPrice = stopPrice;
            }
        }

        // Check take profit
        if (order.getTakeProfitPrice() != 0 && !shouldClose) {
            double takeProfitPrice = order.getTakeProfitPrice();
            if (currentPrice >= takeProfitPrice) {
                shouldClose = true;
                exitPrice = takeProfitPrice;
            }
        }

        // Calculate P&L if position should be closed
        if (shouldClose) {
            double pl = (exitPrice - entryPrice) * order.getAmount();
            totalPL += pl;
        }

    return totalPL - commissions;
    }

    public double getComplexPL() {
        return closedPl + complexOrders.stream().mapToDouble(o -> o.calculatePl(getLastPrice()))
                .sum() - commissions;
    }

    public double getAvailableFunds() {
        return getNetValue() - orders.stream().mapToDouble(t -> Math.abs(t.getAmount()) * t.getOpenPrice() / leverage).sum();
    }

    public double getNetValue() {
        return initialFunds + getPL();
    }

    public double getLastPrice() {
        return currentPrice;
    }

    public Order order(String instrument, boolean buy, int amount, BigDecimal price) {
        SimpleOrder order = new SimpleOrder(orderId++, instrument, getInstant(), price.doubleValue(),
                amount * (buy ? 1 : -1));
        orders.add(order);

        commissions += calculateCommission(order);

        return order;
    }

    public Order complexOrder(String instrument, int amount, BigDecimal price) {
        ComplexOrder order = new ComplexOrder(orderId++, instrument, getInstant(), price.doubleValue(), amount);
        complexOrders.add(order);
        commissions += calculateCommission(order);

        return order;
    }

    public ClosedOrder close(Order order) {
        SimpleOrder simpleOrder = (SimpleOrder) order;
        orders.remove(simpleOrder);
        double price = getLastPrice();
        SimpleClosedOrder closedOrder = new SimpleClosedOrder(simpleOrder, price, getInstant());
        closedOrders.add(closedOrder);
        closedPl += closedOrder.getPl();
        commissions += calculateCommission(order);

        return closedOrder;
    }

    double calculateCommission(Order order) {
        return 1 + Math.abs(order.getAmount()) * 0.005;
    }

}
