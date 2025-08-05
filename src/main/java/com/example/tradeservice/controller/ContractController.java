package com.example.tradeservice.controller;

import com.example.tradeservice.configuration.FinnhubClient;
import com.example.tradeservice.model.ContractModel;
import com.example.tradeservice.model.StockCandles;
import com.example.tradeservice.service.ContractManagerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@CrossOrigin(origins = "*")
@AllArgsConstructor
@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractManagerService contractManagerService;
    private final FinnhubClient finnhubClient;

    @GetMapping("/search")
    List<ContractModel> searchContract(@RequestParam String query) {
        return contractManagerService.searchContract(query);
    }

    @GetMapping("/market-data")
    public void getMarketData(@RequestParam String symbol) {

        long end = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long start = LocalDateTime.now().minusDays(1L).toEpochSecond(ZoneOffset.UTC);

        StockCandles candle = finnhubClient.candle(symbol, "1", start, end);

//        contractManagerService.getMarketData(conid);
    }
}
