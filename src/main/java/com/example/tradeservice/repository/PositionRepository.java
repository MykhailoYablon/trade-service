package com.example.tradeservice.repository;

import com.example.tradeservice.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByConid(Integer conid);
    List<Position> findBySymbolContainingIgnoreCase(String symbol);
}
