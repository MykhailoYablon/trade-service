package com.strategy.trade.handler;

import com.strategy.trade.configuration.FinnhubClient;
import com.strategy.trade.model.TradeData;
import com.strategy.trade.model.WebSocketResponse;
import com.strategy.trade.service.TradeDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class StockTradeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final TradeDataService tradeDataService;
    private WebSocketSession session;
    private final FinnhubClient finnhubClient;

    public StockTradeWebSocketHandler(ObjectMapper objectMapper, TradeDataService tradeDataService,
                                      FinnhubClient finnhubClient) {
        this.objectMapper = objectMapper;
        this.tradeDataService = tradeDataService;
        this.finnhubClient = finnhubClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        log.info("WebSocket connection established: {}", session.getId());

        // Subscribe to symbols immediately after connection (as shown in their docs)
//        subscribeToSymbol("GOOG");
        // Add any other symbols you need
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message: {}", payload);

        try {
            // Parse the incoming WebSocket response
            WebSocketResponse response = objectMapper.readValue(payload, WebSocketResponse.class);

            if ("trade".equals(response.getType()) && response.getData() != null) {
                // Process each trade in the data array
                for (TradeData trade : response.getData()) {
                    log.info("Processing trade: {}", trade);
                    tradeDataService.processRealTimeTrade(trade);
                }
            } else {
                log.info("Received non-trade message type: {}", response.getType());
            }

        } catch (JsonProcessingException e) {
            log.error("Error parsing WebSocket message: {}", payload, e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        this.session = null;
    }

    public Boolean subscribeToSymbol(String symbol) {
        if (Boolean.FALSE.equals(finnhubClient.marketStatus().isIsOpen())) {
            log.info("Market is closed");
            return false;
        }
        if (session != null && session.isOpen()) {
            try {
                // Exact format from their documentation
                String subscribeMessage = String.format(
                        "{\"type\":\"subscribe\",\"symbol\":\"%s\"}",
                        symbol.toUpperCase()
                );
                session.sendMessage(new TextMessage(subscribeMessage));
                log.info("Subscribed to symbol: {}", symbol);
                return true;
            } catch (Exception e) {
                log.error("Error subscribing to symbol: {}", symbol, e);
                return false;
            }
        } else {
            log.warn("Cannot subscribe to {}: WebSocket session is not open", symbol);
            return false;
        }
    }

    public void unsubscribeFromSymbol(String symbol) {
        if (session != null && session.isOpen()) {
            try {
                // Assuming unsubscribe follows same pattern
                String unsubscribeMessage = String.format(
                        "{\"type\":\"unsubscribe\",\"symbol\":\"%s\"}",
                        symbol
                );
                session.sendMessage(new TextMessage(unsubscribeMessage));
                log.info("Unsubscribed from symbol: {}", symbol);
            } catch (Exception e) {
                log.error("Error unsubscribing from symbol: {}", symbol, e);
            }
        } else {
            log.warn("Cannot unsubscribe from {}: WebSocket session is not open", symbol);
        }
    }
}
