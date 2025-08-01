package com.example.tradeservice.service.impl;

import com.example.tradeservice.entity.Position;
import com.example.tradeservice.mapper.ContractMapper;
import com.example.tradeservice.model.ContractHolder;
import com.example.tradeservice.model.ContractModel;
import com.example.tradeservice.repository.PositionRepository;
import com.example.tradeservice.service.ContractManagerService;
import com.example.tradeservice.service.TWSConnectionManager;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Scope("singleton")
@AllArgsConstructor
public class ContractManagerServiceImpl implements ContractManagerService {

    private final TWSConnectionManager tws;

    //    private final ContractRepository contractRepository;
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
