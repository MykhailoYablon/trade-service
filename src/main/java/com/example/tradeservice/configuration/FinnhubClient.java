package com.example.tradeservice.configuration;

import com.example.tradeservice.model.MarketStatus;
import com.example.tradeservice.model.Quote;
import com.example.tradeservice.model.SymbolLookup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static com.example.tradeservice.model.enums.Endpoint.*;

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

    public MarketStatus marketStatus() {
        return restClient.get()
                .uri(MARKET_STATUS.url() + "?token=" + token
                        + "&exchange=US")
                .retrieve()
                .body(MarketStatus.class);
    }
}
