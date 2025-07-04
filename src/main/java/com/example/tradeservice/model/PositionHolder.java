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

//    @Schema(description = "Instrument descriptor of the position")
    private Contract contract;

//    @Schema(description = "Size of the position. In case of short position, the value is negative.")
    private Decimal quantity;

//    @Schema(description = "Average price of the position.")
    private double avgPrice;
}
