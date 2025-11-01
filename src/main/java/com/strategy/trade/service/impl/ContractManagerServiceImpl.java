package com.strategy.trade.service.impl;

import com.strategy.trade.mapper.ContractMapper;
import com.strategy.trade.model.ContractHolder;
import com.strategy.trade.model.ContractModel;
import com.strategy.trade.redis.ContractRepository;
import com.strategy.trade.repository.PositionRepository;
import com.strategy.trade.service.ContractManagerService;
import com.strategy.trade.service.TWSConnectionManager;
import com.strategy.trade.service.TwsResultHolder;
import com.ib.client.Contract;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@Scope("singleton")
@AllArgsConstructor
public class ContractManagerServiceImpl implements ContractManagerService {

    private final TWSConnectionManager tws;

    private final ContractRepository contractRepository;
    private final PositionRepository positionRepository;
    private final ContractMapper contractMapper;


    public List<ContractModel> searchContract(String search) {
        var resultHolder = tws.searchContract(search);
        if (StringUtils.hasLength(resultHolder.getError())) {
            throw new RuntimeException();
        }
        return resultHolder.getResult().stream()
                .map(contractMapper::convertToContract)
                .toList();

    }

    @Override
    public void getMarketData(int conid) {

        TwsResultHolder<ContractHolder> twsResult = tws.requestContractByConid(conid);
            if (!StringUtils.hasLength(twsResult.getError())) {
                contractRepository.save(twsResult.getResult());
                var contractHolder =  twsResult.getResult();
                Contract contract = contractHolder.getContract();
//                tws.subscribeMarketData(contract, false);
                log.info("Subscribed to market data for contract - " + contract.symbol());
            }

    }

//    public ContractHolder getContractHolder(int conid) {
//
//        Optional<Position> contractHolder = positionRepository.findByConid(conid);
//        return contractHolder.orElseGet(() -> {
//            TwsResultHolder<ContractHolder> twsResult = tws.requestContractByConid(conid);
//            if (!StringUtils.hasLength(twsResult.getError())) {
//                contractRepository.save(twsResult.getResult());
//                return twsResult.getResult();
//            }
//            throw new RuntimeException(twsResult.getError());
//        });
//    }
}
