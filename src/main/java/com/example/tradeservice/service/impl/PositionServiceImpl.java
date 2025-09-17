package com.example.tradeservice.service.impl;

import com.example.tradeservice.mapper.PositionMapper;
import com.example.tradeservice.entity.Position;
import com.example.tradeservice.service.PositionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
@Slf4j
public class PositionServiceImpl implements PositionService {

    private final PositionTracker positionTracker;
    private final PositionMapper positionMapper;

    @Transactional(readOnly = true)
    @Override
    public List<Position> getAllPositions() {
        List<Position> positions = new ArrayList<>();
        positionTracker.getAllPositions()
                .forEach(position -> {
                    log.info("Trade: {}", position);
                    Position trade = positionMapper.convertToTrade(position);
                    positions.add(trade);
                });

        return positions;
    }

    @Override
    public Optional<Position> getPositionBySymbol(String symbol) {
        return Optional.of(positionMapper.convertToTrade(positionTracker.getPositionBySymbol(symbol)));
    }

    @Transactional
    @Override
    public Position createPosition(Position position) {


        return null;
    }
}