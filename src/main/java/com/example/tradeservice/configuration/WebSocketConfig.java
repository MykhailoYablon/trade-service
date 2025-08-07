package com.example.tradeservice.configuration;

import com.example.tradeservice.handler.StockTradeWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@Configuration
@EnableWebSocket
public class WebSocketConfig {

    @Bean
    public WebSocketConnectionManager connectionManager(
            @Value("${financial.websocket.url}") String websocketUrl,
            @Value("${financial.api.token}") String token,
            StockTradeWebSocketHandler handler) {

        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                new StandardWebSocketClient(),
                handler,
                websocketUrl + "?token=" + token
        );
        manager.setAutoStartup(true);
        return manager;
    }
}
