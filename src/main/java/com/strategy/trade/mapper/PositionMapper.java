package com.strategy.trade.mapper;

import com.strategy.trade.entity.Position;
import com.strategy.trade.model.PositionHolder;
import com.ib.client.Decimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PositionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conid", expression = "java(positionHolder.getContract().conid())")
    @Mapping(target = "symbol", expression = "java(positionHolder.getContract().symbol())")
    @Mapping(target = "secType", expression = "java(positionHolder.getContract().secType().toString())")
    @Mapping(target = "exchange", expression = "java(positionHolder.getContract().exchange())")
    @Mapping(target = "currency", expression = "java(positionHolder.getContract().currency())")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "avgPrice", source = "avgPrice")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "historicalData", ignore = true)
    Position convertToTrade(PositionHolder positionHolder);

    default BigDecimal map(Decimal value) {
        return value.value();
    }
}
