package com.example.tradeservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContractModel {

    private Integer conid;
    private String symbol;
    private String exchange;
    private String currency;
    private String description;

}
