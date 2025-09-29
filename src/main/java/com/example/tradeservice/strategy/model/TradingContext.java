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
    List<Double> prices;
    List<String> instruments;

    private DoubleSeries profitLoss = new DoubleSeries("pl");
    DoubleSeries fundsHistory = new DoubleSeries("funds");

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
        return closedPl + orders.stream().mapToDouble(t -> t.calculatePl(getLastPrice(t.getInstrument())))
                .sum() - commissions;
    }

    public double getAvailableFunds() {
        return getNetValue() - orders.stream().mapToDouble(t -> Math.abs(t.getAmount()) * t.getOpenPrice() / leverage).sum();
    }

    public double getNetValue() {
        return initialFunds + getPL();
    }

    public double getLastPrice(String instrument) {
        return prices.get(instruments.indexOf(instrument));
    }

    public ClosedOrder close(Order order) {
        SimpleOrder simpleOrder = (SimpleOrder) order;
        orders.remove(simpleOrder);
        double price = getLastPrice(order.getInstrument());
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
