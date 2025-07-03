package com.example.tradeservice.service;

import com.example.tradeservice.model.PositionHolder;
import com.ib.client.Contract;
import com.ib.client.Decimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Scope("singleton")
public class PositionTracker {

    private Map<Integer, PositionHolder> positions = new HashMap<>();

    public void addPosition(PositionHolder positionHolder) {
        if(positionHolder.getQuantity().equals(Decimal.ZERO)) {
            positions.remove(positionHolder.getContract().conid());
        } else {
            positions.put(positionHolder.getContract().conid(), positionHolder);
        }
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
}
