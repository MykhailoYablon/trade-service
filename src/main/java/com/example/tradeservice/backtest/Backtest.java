package com.example.tradeservice.backtest;

import com.example.tradeservice.backtest.series.DoubleSeries;
import com.example.tradeservice.backtest.series.TimeSeries;
import com.example.tradeservice.strategy.AsyncTradingStrategy;
import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
public class Backtest {

    DoubleSeries priceSeries;
    String symbol;

    double deposit;
    double leverage = 1;

    AsyncTradingStrategy strategy;
    TradingContext context;
    Result result;

    Iterator<TimeSeries.Entry<Double>> priceIterator;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Result {
        double pl;
        List<ClosedOrder> orders;
        double initialFund;
        double finalValue;
        double commissions;

    }

    public Backtest(double deposit, DoubleSeries doubleSeries, String symbol) {
        this.deposit = deposit;
        this.priceSeries = doubleSeries;
        this.symbol = symbol;
    }

    public Result run(AsyncTradingStrategy strategy) {
        initialize(strategy);
        while (nextStep()) ;
        return this.result;
    }

    public void initialize(AsyncTradingStrategy strategy) {
        this.strategy = strategy;
        this.context = TradingContext.builder()
                .symbol("symbol")
                .date("date")
                .state(new SymbolTradingState())
                .mode(StrategyMode.BACKTEST)
                .instruments(List.of(this.symbol))
                .initialFunds(deposit)
                .leverage(leverage)
                .build();

        priceIterator = priceSeries.iterator();

        strategy.startStrategy(context);



        nextStep();
    }

    public boolean nextStep() {
        //while redis has records for symbol
//        if (!mPriceIterator.hasNext()) {
//            finish();
//            return false;
//        }

//        TimeSeries.Entry<List<Double>> entry = mPriceIterator.next();

//        context.getProfitLoss().add(context.getPL(), entry.getInstant());
//        context.getFundsHistory().add(context.getAvailableFunds(), entry.getInstant());
        if (context.getAvailableFunds() < 0) {
            finish();
            return false;
        }

//        strategy.onTick();
//
//        context.mHistory.add(entry);

        return true;
    }

    private void finish() {
        for (Order order : new ArrayList<>(context.getOrders())) {
            context.close(order);
        }

//        strategy.onEnd();

        List<ClosedOrder> orders = Collections.unmodifiableList(context.getClosedOrders());
        result = new Result(context.getClosedPl(),
//                context.getProfitLoss(), context.mFundsHistory,
                orders, deposit,
                deposit + context.getClosedPl(), context.getCommissions());
    }
}
