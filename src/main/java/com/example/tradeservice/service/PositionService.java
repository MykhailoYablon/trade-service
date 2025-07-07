package com.example.tradeservice.service;


import com.example.tradeservice.entity.Position;

import java.util.List;
import java.util.Optional;

public interface PositionService {
    List<Position> getAllPositions();
    Optional<Position> getPositionById(int conid);
    Position createPosition(Position position);
}