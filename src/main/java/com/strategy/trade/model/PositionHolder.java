package com.strategy.trade.model;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import lombok.AllArgsConstructor;
import lombok.Data;

//@Schema(description = "Represents an open position")
@Data
@AllArgsConstructor
public class PositionHolder {

    private Contract contract;

    private Decimal quantity;

    private double avgPrice;
}
