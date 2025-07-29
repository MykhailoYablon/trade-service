package com.example.tradeservice.model;

import com.ib.client.Decimal;
import com.ib.client.OrderStatus;
import com.ib.client.Types;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderModel {

    private int orderId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal lmtPrice;
    private String currency;
    private OrderStatus status;
    private Types.Action action;


}
