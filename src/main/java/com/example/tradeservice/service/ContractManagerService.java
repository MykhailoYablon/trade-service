package com.example.tradeservice.service;


import com.example.tradeservice.model.ContractModel;

import java.util.List;

public interface ContractManagerService {
//    ContractHolder getContractHolder(int conid);

    List<ContractModel> searchContract(String search);

    void getMarketData(int conid);
}
