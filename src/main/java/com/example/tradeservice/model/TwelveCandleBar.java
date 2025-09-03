package com.example.tradeservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneId.systemDefault;

@Setter
@Data
public class TwelveCandleBar {

    @CsvBindByName(column = "Stock Symbol")
    @JsonProperty("symbol")
    String symbol;
    @JsonProperty("name")
    String name;
    @JsonProperty("exchange")
    String exchange;
    @JsonProperty("mic_code")
    String mic_code;
    @JsonProperty("currency")
    String currency;
    @CsvBindByName(column = "Date Time")
    @JsonProperty("datetime")
    String datetime;
    @JsonProperty("timestamp")
    int timestamp;
    @JsonProperty("last_quote_at")
    int last_quote_at;
    @JsonProperty("open")
    String myopen;
    @CsvBindByName(column = "High")
    @JsonProperty("high")
    String high;
    @CsvBindByName(column = "Low")
    @JsonProperty("low")
    String low;
    @CsvBindByName(column = "close")
    @JsonProperty("close")
    String close;
    @JsonProperty("volume")
    String volume;
    @JsonProperty("previous_close")
    String previous_close;
    @JsonProperty("change")
    String change;
    @JsonProperty("percent_change")
    String percent_change;
    @JsonProperty("average_volume")
    String average_volume;
    @JsonProperty("rolling_1day_change")
    String rolling_1day_change;
    @JsonProperty("rolling_7day_change")
    String rolling_7day_change;
    @JsonProperty("rolling_period_change")
    String rolling_period_change;
    @JsonProperty("is_market_open")
    boolean is_market_open;
    @JsonProperty("fifty_two_week")
    FiftyTwoWeek fifty_two_week;
    @JsonProperty("extended_change")
    String extended_change;
    @JsonProperty("extended_percent_change")
    String extended_percent_change;
    @JsonProperty("extended_price")
    String extended_price;
    @JsonProperty("extended_timestamp")
    String extended_timestamp;


    public long getEpochMillis() {
        return LocalDateTime.parse(
                datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        ).atZone(systemDefault()).toInstant().toEpochMilli();
    }
}
