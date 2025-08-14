package com.example.tradeservice.service;

import com.ib.client.EClientSocket;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@Scope("singleton")
public class StrategyService {

    private final FinnhubClient finnhubClient;

    @Setter
    @NonNull
    private EClientSocket client;

    public void openingRangeBreakStrategy() {


        // 1. Fetch data for opening 15 min range asynchronously for several symbols
        // 2. Calculate opening 15 min range lows and highs for each symbol
        // 3. Fetch async data in real time and set breakout function if new last_bar.close > opening_range_high
        // 4. Log breakout / create Order
    }
}
