package com.example.tradeservice.service;

import com.example.tradeservice.model.MarketStatus;
import com.example.tradeservice.model.Quote;
import com.example.tradeservice.model.SymbolLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinnhubClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private FinnhubClient finnhubClient;

    private final String testToken = "test-api-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(finnhubClient, "token", testToken);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void search_ShouldReturnSymbolLookup() {
        // Given
        String symbol = "AAPL";
        SymbolLookup expectedLookup = new SymbolLookup();
        expectedLookup.setCount(1L);
        expectedLookup.setResult(new ArrayList<>());

        when(responseSpec.body(SymbolLookup.class)).thenReturn(expectedLookup);

        // When
        SymbolLookup result = finnhubClient.search(symbol);

        // Then
        assertNotNull(result);
        assertEquals(expectedLookup, result);
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(contains("symbol-lookup"));
        verify(requestHeadersUriSpec).uri(contains("token=" + testToken));
        verify(requestHeadersUriSpec).uri(contains("q=AAPL"));
        verify(requestHeadersUriSpec).uri(contains("exchange=US"));
        verify(responseSpec).body(SymbolLookup.class);
    }

    @Test
    void search_ShouldConvertSymbolToUpperCase() {
        // Given
        String symbol = "aapl";
        SymbolLookup expectedLookup = new SymbolLookup();

        when(responseSpec.body(SymbolLookup.class)).thenReturn(expectedLookup);

        // When
        finnhubClient.search(symbol);

        // Then
        verify(requestHeadersUriSpec).uri(contains("q=AAPL"));
    }

    @Test
    void quote_ShouldReturnQuote() {
        // Given
        String symbol = "AAPL";
        Quote expectedQuote = new Quote();
        expectedQuote.setC(150.50f);
        expectedQuote.setH(155.00f);
        expectedQuote.setL(148.00f);
        expectedQuote.setO(149.00f);
        expectedQuote.setPc(149.50f);

        when(responseSpec.body(Quote.class)).thenReturn(expectedQuote);

        // When
        Quote result = finnhubClient.quote(symbol);

        // Then
        assertNotNull(result);
        assertEquals(expectedQuote, result);
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(contains("quote"));
        verify(requestHeadersUriSpec).uri(contains("token=" + testToken));
        verify(requestHeadersUriSpec).uri(contains("symbol=AAPL"));
        verify(responseSpec).body(Quote.class);
    }

    @Test
    void quote_ShouldConvertSymbolToUpperCase() {
        // Given
        String symbol = "aapl";
        Quote expectedQuote = new Quote();

        when(responseSpec.body(Quote.class)).thenReturn(expectedQuote);

        // When
        finnhubClient.quote(symbol);

        // Then
        verify(requestHeadersUriSpec).uri(contains("symbol=AAPL"));
    }

    @Test
    void marketStatus_ShouldReturnMarketStatus() {
        // Given
        MarketStatus expectedStatus = new MarketStatus();
        expectedStatus.setIsOpen(true);
        expectedStatus.setExchange("US");

        when(responseSpec.body(MarketStatus.class)).thenReturn(expectedStatus);

        // When
        MarketStatus result = finnhubClient.marketStatus();

        // Then
        assertNotNull(result);
        assertEquals(expectedStatus, result);
        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(contains("market-status"));
        verify(requestHeadersUriSpec).uri(contains("token=" + testToken));
        verify(requestHeadersUriSpec).uri(contains("exchange=US"));
        verify(responseSpec).body(MarketStatus.class);
    }

    @Test
    void search_ShouldHandleNullSymbol() {
        // Given
        String symbol = null;
        SymbolLookup expectedLookup = new SymbolLookup();

        when(responseSpec.body(SymbolLookup.class)).thenReturn(expectedLookup);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            finnhubClient.search(symbol);
        });
    }

    @Test
    void quote_ShouldHandleNullSymbol() {
        // Given
        String symbol = null;
        Quote expectedQuote = new Quote();

        when(responseSpec.body(Quote.class)).thenReturn(expectedQuote);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            finnhubClient.quote(symbol);
        });
    }

    @Test
    void search_ShouldHandleEmptySymbol() {
        // Given
        String symbol = "";
        SymbolLookup expectedLookup = new SymbolLookup();

        when(responseSpec.body(SymbolLookup.class)).thenReturn(expectedLookup);

        // When
        finnhubClient.search(symbol);

        // Then
        verify(requestHeadersUriSpec).uri(contains("q="));
    }

    @Test
    void quote_ShouldHandleEmptySymbol() {
        // Given
        String symbol = "";
        Quote expectedQuote = new Quote();

        when(responseSpec.body(Quote.class)).thenReturn(expectedQuote);

        // When
        finnhubClient.quote(symbol);

        // Then
        verify(requestHeadersUriSpec).uri(contains("symbol="));
    }

    @Test
    void search_ShouldHandleSpecialCharactersInSymbol() {
        // Given
        String symbol = "BRK.A";
        SymbolLookup expectedLookup = new SymbolLookup();

        when(responseSpec.body(SymbolLookup.class)).thenReturn(expectedLookup);

        // When
        finnhubClient.search(symbol);

        // Then
        verify(requestHeadersUriSpec).uri(contains("q=BRK.A"));
    }

    @Test
    void quote_ShouldHandleSpecialCharactersInSymbol() {
        // Given
        String symbol = "BRK.A";
        Quote expectedQuote = new Quote();

        when(responseSpec.body(Quote.class)).thenReturn(expectedQuote);

        // When
        finnhubClient.quote(symbol);

        // Then
        verify(requestHeadersUriSpec).uri(contains("symbol=BRK.A"));
    }
}
