package com.example.tradeservice.service.impl;

import com.example.tradeservice.entity.DataRequest;
import com.example.tradeservice.entity.Position;
import com.example.tradeservice.model.PositionHolder;
import com.example.tradeservice.model.enums.RequestStatus;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.repository.DataRequestRepository;
import com.example.tradeservice.repository.PositionRepository;
import com.ib.client.Contract;
import com.ib.client.Decimal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Service
@Scope("singleton")
@AllArgsConstructor
public class PositionTracker {

    private final PositionRepository positionRepository;
    private final DataRequestRepository dataRequestRepository;

    private Map<Integer, PositionHolder> positions;

    public void addPosition(PositionHolder positionHolder) {
        if(positionHolder.getQuantity().equals(Decimal.ZERO)) {
            positions.remove(positionHolder.getContract().conid());
        } else {
            positions.put(positionHolder.getContract().conid(), positionHolder);
            processPosition(positionHolder.getContract(), positionHolder.getQuantity(), positionHolder.getAvgPrice());
        }
    }

    // Step 1: Save/Update positions when receiving from IB
    public void processPosition(Contract contract, Decimal quantity, double avgPrice) {
        Position position = positionRepository.findByConid(contract.conid())
                .orElse(new Position());

        // Update position data
        position.setConid(contract.conid());
        position.setSymbol(contract.symbol());
        position.setSecType(contract.secType().toString());
        position.setExchange(contract.exchange());
        position.setCurrency(contract.currency());
        position.setQuantity(new BigDecimal(quantity.toString()));
        position.setAvgPrice(new BigDecimal(avgPrice));
        position.setUpdatedAt(LocalDateTime.now());

        if (position.getId() == null) {
            position.setCreatedAt(LocalDateTime.now());
        }

        positionRepository.save(position);
        log.info("Position saved/updated: {} - {} shares", position.getSymbol(), position.getQuantity());
    }

    public Collection<PositionHolder> getAllPositions() {
        return positions.values();
    }

    public PositionHolder getPositionByContract(Contract contract) {
        return positions.get(contract.conid());
    }

    public PositionHolder getPositionByConid(int conid) {
        return positions.get(conid);
    }

    // Step 2: Save DataRequest when making historical data request
    public void createDataRequest(int reqId, Contract contract, String duration, String barSize) {
        Position position = positionRepository.findByConid(contract.conid())
                .orElseThrow(() -> new RuntimeException("Position not found for conid: " + contract.conid()));

        DataRequest dataRequest = new DataRequest();
        dataRequest.setReqId(reqId);
        dataRequest.setPosition(position);
        dataRequest.setTimeframe(TimeFrame.fromIbFormat(barSize));
        dataRequest.setDuration(duration);
        dataRequest.setStatus(RequestStatus.PENDING);
        dataRequest.setRequestedAt(LocalDateTime.now());

        dataRequestRepository.save(dataRequest);
        log.info("DataRequest created: reqId={}, symbol={}, timeframe={}",
                reqId, position.getSymbol(), dataRequest.getTimeframe());
    }
}
