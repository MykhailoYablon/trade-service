package com.strategy.trade.redis;

import com.strategy.trade.model.ContractHolder;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends CrudRepository<ContractHolder, Integer> {

    ContractHolder findContractHolderByOptionChainRequestId(Integer optionChainRequestId);

}
