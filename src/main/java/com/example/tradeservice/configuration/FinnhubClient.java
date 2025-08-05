package com.example.tradeservice.configuration;

import com.example.tradeservice.model.MarketStatus;
import com.example.tradeservice.model.StockCandles;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;

import static com.example.tradeservice.model.enums.Endpoint.*;

@NoArgsConstructor
@Getter
@Setter
@Component
public class FinnhubClient {


    @Autowired
    private RestClient restClient;
    private String token = "token";

    public StockCandles candle(String symbol, String resolution, long startEpoch, long endEpoch) {
        return restClient.get()
                .uri(SYMBOL_LOOKUP.url() + "?token=" + token
                        + "&q=" + symbol.toUpperCase())
                .retrieve()
                .body(StockCandles.class);
    }

    public MarketStatus marketStatus() {
        return restClient.get()
                .uri(MARKET_STATUS.url() + "?token=" + token
                        + "&exchange=US")
                .retrieve()
                .body(MarketStatus.class);
    }

    private static final class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDate localDate) throws IOException {
            jsonWriter.value(localDate.toString());
        }

        @Override
        public LocalDate read(final JsonReader jsonReader) throws IOException {
            return LocalDate.parse(jsonReader.nextString());
        }
    }
}
