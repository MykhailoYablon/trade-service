package com.example.tradeservice.service;

import com.example.tradeservice.model.Trade;
import com.example.tradeservice.repository.TradeRepository;
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
public class TradeServiceImpl implements TradeService {
    private final TradeRepository tradeRepository;
    private final PositionTracker positionTracker;


    @Transactional(readOnly = true)
    @Override
    public List<Trade> getAllTrades() {
        List<Trade> trades = new ArrayList<>();
        positionTracker.getAllPositions()
                .forEach(position -> {

                    log.info("Trade: {}", position);

                    Trade trade = Trade.builder()
                            .symbol(position.getContract().symbol())
                            .quantity(position.getQuantity().value())
                            .price(position.getAvgPrice())

                            .build();
                    trades.add(trade);
                });
        return trades;
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