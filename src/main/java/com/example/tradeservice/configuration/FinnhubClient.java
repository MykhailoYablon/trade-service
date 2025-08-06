package com.example.tradeservice.configuration;

import com.example.tradeservice.model.MarketStatus;
import com.example.tradeservice.model.Quote;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static com.example.tradeservice.model.enums.Endpoint.MARKET_STATUS;
import static com.example.tradeservice.model.enums.Endpoint.QUOTE;

@NoArgsConstructor
@Getter
@Setter
@Component
public class FinnhubClient {


    @Autowired
    private RestClient restClient;
    @Value("${financial.api.token}")
    private String token;

    public Quote quote(String symbol) {
        return restClient.get()
                .uri(QUOTE.url() + "?token=" + token
                        + "&symbol=" + symbol.toUpperCase())
                .retrieve()
                .body(Quote.class);
    }

    public MarketStatus marketStatus() {
        return restClient.get()
                .uri(MARKET_STATUS.url() + "?token=" + token
                        + "&exchange=US")
                .retrieve()
                .body(MarketStatus.class);
    }
}
