package com.strategy.trade.strategy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BreakoutData(BigDecimal breakoutPrice, LocalDateTime breakoutTime, BigDecimal breakoutHigh) {
}
