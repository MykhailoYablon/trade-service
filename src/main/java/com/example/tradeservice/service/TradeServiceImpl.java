package com.example.tradeservice.service;

import com.example.tradeservice.model.Trade;
import com.example.tradeservice.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TradeServiceImpl implements TradeService {
    private final TradeRepository tradeRepository;

    public TradeServiceImpl(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Trade> getAllTrades() {
        return tradeRepository.findAll();
    }

    @Override
    public Optional<Trade> getTradeById(Long id) {
        return tradeRepository.findById(id);
    }

    @Transactional
    @Override
    public Trade createTrade(Trade trade) {
        return tradeRepository.save(trade);
    }

    @Transactional
    @Override
    public Optional<Trade> updateTrade(Long id, Trade tradeDetails) {
        return tradeRepository.findById(id)
                .map(trade -> {
                    trade.setSymbol(tradeDetails.getSymbol());
                    trade.setQuantity(tradeDetails.getQuantity());
                    trade.setPrice(tradeDetails.getPrice());
                    return tradeRepository.save(trade);
                });
    }

    @Override
    @Transactional
    public boolean deleteTrade(Long id) {
        return tradeRepository.findById(id)
                .map(trade -> {
                    tradeRepository.delete(trade);
                    return true;
                })
                .orElse(false);
    }
} 