package com.example.tradeservice.service;

import com.example.tradeservice.configuration.FinnhubClient;
import com.example.tradeservice.configuration.TwelveDataClient;
import com.example.tradeservice.handler.StockTradeWebSocketHandler;
import com.example.tradeservice.handler.TradeUpdatedEvent;
import com.example.tradeservice.model.StockResponse;
import com.example.tradeservice.model.TradeData;
import com.example.tradeservice.model.enums.TimeFrame;
import com.ib.client.EClientSocket;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Scope("singleton")
public class StrategyService {

    private final FinnhubClient finnhubClient;
    private final TwelveDataClient twelveDataClient;
    private final StockTradeWebSocketHandler handler;
    private final TradeDataService tradeDataService;
    private final RedisTemplate<String, TradeData> redisTemplate;

    @Setter
    @NonNull
    private EClientSocket client;

    public StrategyService(FinnhubClient finnhubClient, TwelveDataClient twelveDataClient,
                           StockTradeWebSocketHandler handler, TradeDataService tradeDataService,
                           RedisTemplate<String, TradeData> redisTemplate) {
        this.finnhubClient = finnhubClient;
        this.twelveDataClient = twelveDataClient;
        this.handler = handler;
        this.tradeDataService = tradeDataService;
        this.redisTemplate = redisTemplate;
    }

    @EventListener
    public void handleOrderCompletedEvent(TradeUpdatedEvent event) {
        TradeData trade = event.getTrade();
        log.info("Trade updated: {} - ${} (volume: {}, time: {})",
                trade.getSymbol(), trade.getPrice(), trade.getVolume(), trade.getDateTime());
    }

    //    @Scheduled("")
    public void openingRangeBreakStrategy() {

        String symbol = "KYIV";

        // 1. Fetch data for opening 15 min range asynchronously for several symbols
        //get last 15 min candle
        StockResponse stockResponse = twelveDataClient.timeSeries(symbol, TimeFrame.FIFTEEN_MIN);

        log.info("Stock response - {}", stockResponse);

        Double high = stockResponse.getValues().stream()
                .map(value -> Pair.of(value.getHigh(), value.getLow()))
                .collect(())

                .map(value -> Double.valueOf(value.getHigh()))
                .max(Double::compare)
                .get();




        // 2. Calculate opening 15 min range lows and highs for each symbol
//        tradeHistory.stream()
//                .min(trade -> trade.getPrice())

        // 3. Fetch async data in real time and set breakout function if new last_bar.close > opening_range_high
        // 4. Log breakout / create Order

        // 5. Unsubscribe from symbol if break happened
        boolean isBreak = false;
        if (isBreak) {
            handler.unsubscribeFromSymbol(symbol);
        }
    }
}
