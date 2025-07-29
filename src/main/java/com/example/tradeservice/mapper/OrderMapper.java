package com.example.tradeservice.mapper;

import com.example.tradeservice.model.OrderModel;
import com.example.tradeservice.model.OrderHolder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderId", expression = "java(orderHolder.getOrder().orderId())")
    @Mapping(target = "symbol", expression = "java(orderHolder.getContract().symbol())")
    @Mapping(target = "currency", expression = "java(orderHolder.getContract().currency())")
    @Mapping(target = "quantity", expression = "java(orderHolder.getOrder().totalQuantity().value())")
    @Mapping(target = "action", expression = "java(orderHolder.getOrder().action())")
    @Mapping(target = "lmtPrice", expression = "java(BigDecimal.valueOf(orderHolder.getOrder().lmtPrice()))")
    @Mapping(target = "status", expression = "java(orderHolder.getOrderState().status())")
    OrderModel convertToOrder(OrderHolder orderHolder);
}
