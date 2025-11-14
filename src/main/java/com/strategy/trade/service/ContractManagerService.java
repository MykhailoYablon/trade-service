package com.strategy.trade.service;


import com.strategy.trade.model.ContractModel;

import java.util.List;

public interface ContractManagerService {
//    ContractHolder getContractHolder(int conid);

    List<ContractModel> searchContract(String search);

    void getMarketData(int conid);
}
