package com.example.tradeservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StockResponse {
    private Meta meta;
    private List<Value> values;
    private String status;

    @Data
    public static class Meta {
        private String symbol;
        private String interval;
        private String currency;

        @JsonProperty("exchange_timezone")
        private String exchangeTimezone;

        private String exchange;

        @JsonProperty("mic_code")
        private String micCode;

        private String type;
    }

    @Data
    public static class Value {
        private String datetime;
        private String open;
        private String high;
        private String low;
        private String close;
        private String volume;
    }
}
