package com.strategy.trade.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeData {
    // Getters and setters
    @JsonProperty("s")
    private String symbol;

    @JsonProperty("p")
    private Double price;

    @JsonProperty("t")
    private Long timestamp;

    @JsonProperty("v")
    private Double volume;

    @JsonProperty("c")
    private List<String> conditions;

    public TradeData(String symbol, Double price, Long timestamp, Double volume) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
        this.volume = volume;
    }

    // Utility methods
    public Instant getInstant() {
        return timestamp != null ? Instant.ofEpochMilli(timestamp) : null;
    }

    public LocalDateTime getDateTime() {
        return timestamp != null ? LocalDateTime.ofInstant(getInstant(), ZoneOffset.UTC) : null;
    }

    @Override
    public String toString() {
        return String.format("TradeData{symbol='%s', price=%.2f, volume=%.6f, timestamp=%d}",
                symbol, price, volume, timestamp);
    }
}
