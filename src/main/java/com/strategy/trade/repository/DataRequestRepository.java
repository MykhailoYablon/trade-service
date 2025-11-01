package com.strategy.trade.repository;

import com.strategy.trade.entity.DataRequest;
import com.strategy.trade.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataRequestRepository extends JpaRepository<DataRequest, Long> {
    List<DataRequest> findByStatus(RequestStatus status);
    Optional<DataRequest> findByReqId(Integer reqId);
}
