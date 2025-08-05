package com.example.tradeservice.controller;

import com.example.tradeservice.configuration.FinnhubClient;
import com.example.tradeservice.model.MarketStatus;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@AllArgsConstructor
@RestController
@RequestMapping("/markets")
public class MarketController {

    private final FinnhubClient finnhubClient;


    @GetMapping("/status")
    public MarketStatus getMarketStatus() {
        return finnhubClient.marketStatus();
    }
}
