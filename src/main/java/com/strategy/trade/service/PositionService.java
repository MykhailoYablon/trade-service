package com.strategy.trade.service;


import com.strategy.trade.entity.Position;

import java.util.List;
import java.util.Optional;

public interface PositionService {
    List<Position> getAllPositions();
    Optional<Position> getPositionBySymbol(String symbol);
    Position createPosition(Position position);
}