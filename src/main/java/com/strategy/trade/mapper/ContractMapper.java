package com.strategy.trade.mapper;

import com.strategy.trade.model.ContractModel;
import com.ib.client.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(target = "conid", expression = "java(contract.conid())")
    @Mapping(target = "symbol", expression = "java(contract.symbol())")
    @Mapping(target = "exchange", expression = "java(contract.exchange())")
    @Mapping(target = "currency", expression = "java(contract.currency())")
//    @Mapping(target = "price", expression = "java(contract.currency())")
    @Mapping(target = "description", expression = "java(contract.description())")
    ContractModel convertToContract(Contract contract);
}
