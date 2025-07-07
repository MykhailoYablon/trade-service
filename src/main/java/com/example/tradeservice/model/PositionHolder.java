package com.example.tradeservice.model;

import com.ib.client.Contract;
//import io.swagger.v3.oas.annotations.media.Schema;
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
