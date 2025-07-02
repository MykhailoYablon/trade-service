package com.example.tradeservice.repository;

import com.example.tradeservice.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
} 