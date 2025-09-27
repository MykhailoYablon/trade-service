package com.example.tradeservice.backtest;

import com.example.tradeservice.strategy.AsyncTradingStrategy;
import com.example.tradeservice.strategy.enums.StrategyMode;
import com.example.tradeservice.strategy.model.ClosedOrder;
import com.example.tradeservice.strategy.model.Order;
import com.example.tradeservice.strategy.model.SymbolTradingState;
import com.example.tradeservice.strategy.model.TradingContext;
import com.example.tradeservice.strategy.series.TimeSeries;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class Backtest {

    double deposit;
    double leverage = 1;

    AsyncTradingStrategy strategy;
    TradingContext context;
    Result result;

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

    public Backtest(double deposit) {
        this.deposit = deposit;
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
                .build();

        strategy.startStrategy(context);

        nextStep();
    }

    public boolean nextStep() {
        //while redis has records for symbol
        if (!mPriceIterator.hasNext()) {
            finish();
            return false;
        }

        TimeSeries.Entry<List<Double>> entry = mPriceIterator.next();

        context.getProfitLoss().add(context.getPL(), entry.getInstant());
        context.getFundsHistory().add(context.getAvailableFunds(), entry.getInstant());
        if (context.getAvailableFunds() < 0) {
            finish();
            return false;
        }

        strategy.onTick();

        context.mHistory.add(entry);

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
