package com.example.tradeservice.repository;

import com.example.tradeservice.model.ContractHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<ContractHolder, Integer> {

    ContractHolder findContractHolderByOptionChainRequestId(Integer optionChainRequestId);

}
