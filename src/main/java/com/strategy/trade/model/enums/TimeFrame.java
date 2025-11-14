package com.strategy.trade.model.enums;

import lombok.Getter;

public enum TimeFrame {
    ONE_MIN("1 min", "1min"),
    THREE_MIN("3 mins", ""),
    FIVE_MIN("5 mins", "5min"),
    FIFTEEN_MIN("15 mins", "15min"),
    THIRTY_MIN("30 mins", "30min"),
    ONE_HOUR("1 hour", "1h"),
    ONE_DAY("1 day", "1day");

    @Getter
    private final String ibFormat;
    @Getter
    private final String twelveFormat;

    TimeFrame(String ibFormat, String twelveFormat) {
        this.ibFormat = ibFormat;
        this.twelveFormat = twelveFormat;
    }

    public static TimeFrame fromIbFormat(String ibFormat) {
        for (TimeFrame tf : values()) {
            if (tf.ibFormat.equals(ibFormat)) {
                return tf;
            }
        }
        throw new IllegalArgumentException("Unknown timeframe: " + ibFormat);
    }
}
