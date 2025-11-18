package com.strategy.trade.model.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDefinedAttributes {
    private String name;
    private String category;
    private String assetClass;
    private String risk;
    private String term;
    private String liquidity;
}
