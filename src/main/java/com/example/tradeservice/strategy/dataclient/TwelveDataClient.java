package com.example.tradeservice.strategy.dataclient;

import com.example.tradeservice.model.StockResponse;
import com.example.tradeservice.model.TwelveCandleBar;
import com.example.tradeservice.model.enums.TimeFrame;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static com.example.tradeservice.model.enums.Endpoint.TIME_SERIES;
import static com.example.tradeservice.model.enums.Endpoint.TWELVE_QUOTE;

@NoArgsConstructor
@Getter
@Setter
@Component("twelveData")
@Qualifier("twelveData")
@Slf4j
@Primary
public class TwelveDataClient implements StockDataClient {

    @Autowired
    private RestClient restClient;
    @Value("${financial.twelve.api.token}")
    private String token;

    public StockResponse timeSeries(String symbol, String timeFrame, String startDate, String endDate) {
        return restClient.get()
                .uri(TIME_SERIES.url() + "?apikey=" + token
                        + "&symbol=" + symbol.toUpperCase()
                        + "&interval=" + timeFrame
                        + "&start_date=" + startDate
                        + "&end_date=" + endDate
                )
                .retrieve()
                .body(StockResponse.class);
    }

    public String csvTimeSeries(String symbol) {
        return restClient.get()
                .uri(TIME_SERIES.url() + "?apikey=" + token
                        + "&symbol=" + symbol.toUpperCase()
                        + "&interval=" + TimeFrame.ONE_DAY.getTwelveFormat()
                        + "&format=CSV"
                )
                .retrieve()
                .body(String.class);
    }

    @Override
    public void initializeCsvForDay(String symbol, String date) {
        //do nothing
    }

    @Override
    public TwelveCandleBar quoteWithInterval(String symbol, TimeFrame timeFrame, String date) {
        log.info("Fetching candle for symbol - {} with timeframe - {}", symbol, timeFrame);
        return restClient.get()
                .uri(TWELVE_QUOTE.url() + "?apikey=" + token
                        + "&symbol=" + symbol.toUpperCase()
                        + "&interval=" + timeFrame.getTwelveFormat()
                )
                .retrieve()
                .body(TwelveCandleBar.class);
    }

}
