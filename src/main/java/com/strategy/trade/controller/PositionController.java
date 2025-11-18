package com.strategy.trade.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.strategy.trade.entity.HistoricalData;
import com.strategy.trade.entity.Position;
import com.strategy.trade.model.ai.Holding;
import com.strategy.trade.model.enums.TimeFrame;
import com.strategy.trade.repository.HistoricalDataRepository;
import com.strategy.trade.service.PositionService;
import com.strategy.trade.strategy.dataclient.TwelveDataClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/positions")
@AllArgsConstructor
public class PositionController {
    private final PositionService positionService;
    private final HistoricalDataRepository historicalDataRepository;
    private final TwelveDataClient twelveDataClient;
    private final ObjectMapper objectMapper;
    private final Client client;

    @GetMapping("/all")
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/ai")
    public String getAiSummary() {
        Holding holding;
        try {
            holding = objectMapper.readValue(new File("src/main/resources/holdings.json"),
                    Holding.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        holding.getPositions().forEach(position -> {
            //read last price
            String currentPrice = twelveDataClient.price(position.getSymbol());
            position.setCurrentPrice(currentPrice);
        });

        //add analyze of positions based on ai response
        // The client gets the API key from the environment variable `GEMINI_API_KEY`.
        GenerateContentResponse response;
        try {
            response = client.models.generateContent(
                    "gemini-2.5-flash",
                    """
                            Imagine you are a financial advisor with 50 years of experience.
                            Conduct an analysis of the current portfolio provided in JSON format.
                            In your analysis, consider the key risks arising from the current asset allocation and market conditions,
                            as well as potential issues that may impact the portfolio's performance.
                            Analyze different types of profitability and unrealized P&L, perform comparisons, and provide recommendations.
                            Identify promising or underutilized opportunities that could be beneficial in the future,
                            considering current and projected market trends.
                            Provide practical recommendations for rebalancing the portfolio and suggest growth strategies
                            for both the mid-term and short-term. The report should be concise (up to 700 words)
                            and written in clear and accessible language so that both financial professionals and individuals
                            with limited financial knowledge can easily understand it.
                            """ + objectMapper.writeValueAsString(holding),
                    null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        System.out.println(response.text());
        return "Doing Great!";
    }

    @GetMapping
    public ResponseEntity<Position> getPositionBySymbol(@RequestParam String symbol) {
        return positionService.getPositionBySymbol(symbol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{conid}/historical/{timeframe}")
    public List<HistoricalData> getHistoricalData(
            @PathVariable Integer conid,
            @PathVariable TimeFrame timeframe,
            @RequestParam(defaultValue = "3") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);

        return historicalDataRepository
                .findRecentData(conid, timeframe, since);
    }
}