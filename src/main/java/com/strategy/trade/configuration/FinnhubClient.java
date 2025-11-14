package com.strategy.trade.configuration;

import com.strategy.trade.model.MarketStatus;
import com.strategy.trade.model.Quote;
import com.strategy.trade.model.SymbolLookup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static com.strategy.trade.model.enums.Endpoint.*;

@NoArgsConstructor
@Getter
@Setter
@Component
public class FinnhubClient {

    @Autowired
    private RestClient restClient;
    @Value("${financial.api.token}")
    private String token;

    public SymbolLookup search(String symbol) {
        return restClient.get()
                .uri(SYMBOL_LOOKUP.url() + "?token=" + token
                        + "&q=" + symbol.toUpperCase()
                        + "&exchange=US")
                .retrieve()
                .body(SymbolLookup.class);
    }

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
