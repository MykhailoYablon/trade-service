package com.example.tradeservice.strategy.model;

import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.series.DoubleSeries;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class TradingContext {
    private String symbol;
    private String date;
    private SymbolTradingState state;
    private StrategyMode mode;
    private DoubleSeries profitLoss = new DoubleSeries("pl");
    DoubleSeries fundsHistory = new DoubleSeries("funds");
    double initialFunds;
    double commissions;
    double leverage;
    double closedPl = 0;
//    List<SimpleClosedOrder> mClosedOrders = new ArrayList<>();
    List<ClosedOrder> closedOrders = new ArrayList<>();
    List<Double> prices;
    List<String> instruments;
    Instant instant;

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
        Order simpleOrder = (Order) order;
        orders.remove(simpleOrder);
        double price = getLastPrice(order.getInstrument());
        ClosedOrder closedOrder = new ClosedOrder(simpleOrder, price, getInstant());
        closedOrders.add(closedOrder);
        closedPl += closedOrder.getPl();
        commissions += calculateCommission(order);

        return closedOrder;
    }

    double calculateCommission(Order order) {
        return 1 + Math.abs(order.getAmount()) * 0.005;
    }

}
