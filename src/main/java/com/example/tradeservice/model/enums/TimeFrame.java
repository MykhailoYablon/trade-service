package com.example.tradeservice.model.enums;

public enum TimeFrame {
    ONE_MIN("1 min"),
    THREE_MIN("3 mins"),
    FIVE_MIN("5 mins"),
    FIFTEEN_MIN("15 mins"),
    THIRTY_MIN("30 mins"),
    ONE_HOUR("1 hour"),
    ONE_DAY("1 day");

    private final String ibFormat;

    TimeFrame(String ibFormat) {
        this.ibFormat = ibFormat;
    }

    public String getIbFormat() {
        return ibFormat;
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
