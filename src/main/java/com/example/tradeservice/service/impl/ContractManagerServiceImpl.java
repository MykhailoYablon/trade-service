package com.example.tradeservice.service.impl;

import com.example.tradeservice.entity.Position;
import com.example.tradeservice.model.ContractHolder;
import com.example.tradeservice.repository.PositionRepository;
import com.example.tradeservice.service.ContractManagerService;
import com.example.tradeservice.service.TwsResultHolder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@Scope("singleton")
public class ContractManagerServiceImpl implements ContractManagerService {

//    private final ContractRepository contractRepository;
    private final PositionRepository positionRepository;

    public ContractManagerServiceImpl(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public ContractHolder getContractHolder(int conid) {

        Optional<Position> position = positionRepository.findByConid(conid);
        return null;
//        return contractHolder.orElseGet(() -> {
//            TwsResultHolder<ContractHolder> twsResult = tws.requestContractByConid(conid);
//            if (!StringUtils.hasLength(twsResult.getError())) {
//                contractRepository.save(twsResult.getResult());
//                return twsResult.getResult();
//            }
//            throw new RuntimeException(twsResult.getError());
//        });
    }
}
