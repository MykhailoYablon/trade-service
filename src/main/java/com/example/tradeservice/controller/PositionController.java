package com.example.tradeservice.controller;

import com.example.tradeservice.entity.HistoricalData;
import com.example.tradeservice.entity.Position;
import com.example.tradeservice.model.enums.TimeFrame;
import com.example.tradeservice.repository.HistoricalDataRepository;
import com.example.tradeservice.service.PositionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/positions")
@AllArgsConstructor
public class PositionController {
    private final PositionService positionService;
    private final HistoricalDataRepository historicalDataRepository;

    @GetMapping("/all")
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
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