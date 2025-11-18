package com.strategy.trade.model.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Holding {
    private String type;
    private String cashBalance;
    private String netWorth;
    private String timeWeightedReturn;
    private String unrealizedProfitLoss;
    private UserDefinedAttributes userDefinedAttributes;
    private List<Position> positions;
}

