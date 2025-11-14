package com.strategy.trade.backtest.series;

import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;



public class TimeSeries<T> implements Iterable<TimeSeries.Entry<T>> {
    public static class Entry<T> {
        T value;
        @Getter
        Instant instant;

        public Entry(T t, Instant instant) {
            value = t;
            this.instant = instant;
        }

        public T getItem() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (!instant.equals(entry.instant)) return false;
            if (value != null ? !value.equals(entry.value) : entry.value != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = value != null ? value.hashCode() : 0;
            result = 31 * result + instant.hashCode();
            return result;
        }

        @Override public String toString() {
            return "Entry{" +
                "mInstant=" + instant +
                ", mT=" + value +
                '}';
        }
    }

    List<Entry<T>> data;

    public TimeSeries() {
        data = new ArrayList<>();
    }

    protected TimeSeries(List<Entry<T>> data) {
        this.data = data;
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean add(Entry<T> tEntry) {
        return data.add(tEntry);
    }

    public void add(T item, Instant instant) {
        add(new Entry<T>(item, instant));
    }

    public Stream<Entry<T>> stream() {
        return data.stream();
    }

    @Override public Iterator<Entry<T>> iterator() {
        return data.iterator();
    }

    public List<Entry<T>> getData() {
        return Collections.unmodifiableList(data);
    }

    public Entry<T> get(int index) {
        return data.get(index);
    }

    public <F> TimeSeries<F> map(Function<T, F> f) {
        List<Entry<F>> newEntries = new ArrayList<>(size());
        for (Entry<T> entry : data) {
            newEntries.add(new Entry<>(f.apply(entry.value), entry.instant));
        }
        return new TimeSeries<>(newEntries);
    }

    public boolean isAscending() {
        return size() <= 1 || get(0).getInstant().isBefore(get(1).instant);
    }

    public TimeSeries<T> toAscending() {
        if (!isAscending()) {
            return reverse();
        }
        return this;
    }

    public TimeSeries<T> toDescending() {
        if (isAscending()) {
            return reverse();
        }
        return this;
    }

    public TimeSeries<T> reverse() {
        ArrayList<Entry<T>> entries = new ArrayList<>(data);
        Collections.reverse(entries);
        return new TimeSeries<>(entries);
    }

    @Override public String toString() {
        return data.isEmpty() ? "TimeSeries{empty}" :
            "TimeSeries{" +
                "from=" + data.get(0).getInstant() +
                ", to=" + data.get(size() - 1).getInstant() +
                ", size=" + data.size() +
                '}';
    }
}
