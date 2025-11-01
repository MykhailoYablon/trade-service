package com.strategy.trade.backtest.series;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DoubleSeries extends TimeSeries<Double> {
    String name;

    public DoubleSeries(List<Entry<Double>> data, String name) {
        super(data);
        this.name = name;
    }

    public DoubleSeries(String name) {
        super();
        this.name = name;
    }

    @Override public DoubleSeries toAscending() {
        return new DoubleSeries(super.toAscending().data, getName());
    }

    @Override public DoubleSeries toDescending() {
        return new DoubleSeries(super.toDescending().data, getName());
    }

    @Override public String toString() {
        return data.isEmpty() ? "DoubleSeries{empty}" :
            "DoubleSeries{" +
                "mName=" + name +
                ", from=" + data.get(0).getInstant() +
                ", to=" + data.get(data.size() - 1).getInstant() +
                ", size=" + data.size() +
                '}';
    }
}
