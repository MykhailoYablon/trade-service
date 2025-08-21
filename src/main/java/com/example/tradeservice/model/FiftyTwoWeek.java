package com.example.tradeservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FiftyTwoWeek {

    @JsonProperty("low")
    String low;
    @JsonProperty("high")
    String high;
    @JsonProperty("low_change")
    String low_change;
    @JsonProperty("high_change")
    String high_change;
    @JsonProperty("low_change_percent")
    String low_change_percent;
    @JsonProperty("high_change_percent")
    String high_change_percent;
    @JsonProperty("range")
    String range;
}
