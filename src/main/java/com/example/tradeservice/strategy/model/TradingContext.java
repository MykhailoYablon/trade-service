package com.example.tradeservice.strategy.model;

import com.example.tradeservice.backtest.ClosedOrder;
import com.example.tradeservice.backtest.Order;
import com.example.tradeservice.backtest.SimpleClosedOrder;
import com.example.tradeservice.backtest.SimpleOrder;
import com.example.tradeservice.backtest.series.DoubleSeries;
import com.example.tradeservice.strategy.enums.StrategyMode;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class TradingContext {
    Instant instant;
    Double price;
    List<String> instruments;

    public DoubleSeries profitLoss = new DoubleSeries("pl");
    public DoubleSeries fundsHistory = new DoubleSeries("funds");
    public DoubleSeries mHistory;

    private String symbol;
    private String date;
    private SymbolTradingState state;
    private StrategyMode mode;


    double initialFunds;
    double commissions;
    int orderId = 1;
    double leverage;
    double closedPl = 0;
    //    List<SimpleClosedOrder> mClosedOrders = new ArrayList<>();
    List<ClosedOrder> closedOrders = new ArrayList<>();


    List<Order> orders = new ArrayList<>();

    public double getPL() {
        return closedPl + orders.stream().mapToDouble(t -> t.calculatePl(getLastPrice()))
                .sum() - commissions;
    }

    public double getAvailableFunds() {
        return getNetValue() - orders.stream().mapToDouble(t -> Math.abs(t.getAmount()) * t.getOpenPrice() / leverage).sum();
    }

    public double getNetValue() {
        return initialFunds + getPL();
    }

    public double getLastPrice() {
        return price;
    }

    public Order order(String instrument, boolean buy, int amount) {

        double price = getLastPrice();
        SimpleOrder order = new SimpleOrder(orderId++, instrument, getInstant(), price, amount * (buy ? 1 : -1));
        orders.add(order);

        commissions += calculateCommission(order);

        return order;
    }

    public ClosedOrder close(Order order) {
        SimpleOrder simpleOrder = (SimpleOrder) order;
        orders.remove(simpleOrder);
//        double price = getLastPrice(order.getInstrument());
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
