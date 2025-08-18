package com.example.tradeservice.configuration;

import com.example.tradeservice.model.StockResponse;
import com.example.tradeservice.model.enums.TimeFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import static com.example.tradeservice.model.enums.Endpoint.TIME_SERIES;

public class TwelveDataClient {

    @Autowired
    private RestClient restClient;
    @Value("${financial.twelve.api.token}")
    private String token;

    public StockResponse timeSeries(String symbol, TimeFrame timeFrame) {
        return restClient.get()
                .uri(TIME_SERIES.url() + "?apikey=" + token
                        + "&symbol=" + symbol.toUpperCase()
                        + "&interval=" + timeFrame.getIbFormat()
                )
                .retrieve()
                .body(StockResponse.class);
    }

}
