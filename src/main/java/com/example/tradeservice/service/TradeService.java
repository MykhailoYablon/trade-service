package com.example.tradeservice.service;


import com.example.tradeservice.model.Trade;

import java.util.List;
import java.util.Optional;

public interface TradeService {
    List<Trade> getAllTrades();
    Optional<Trade> getTradeById(Long id);
    Trade createTrade(Trade trade);
    Optional<Trade> updateTrade(Long id, Trade tradeDetails);
    boolean deleteTrade(Long id);
} 