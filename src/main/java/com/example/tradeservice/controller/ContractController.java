package com.example.tradeservice.controller;

import com.example.tradeservice.model.ContractModel;
import com.example.tradeservice.service.ContractManagerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@AllArgsConstructor
@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractManagerService contractManagerService;

    @GetMapping("/search")
    List<ContractModel> searchContract(@RequestParam String query) {
        return contractManagerService.searchContract(query);
    }

    @GetMapping("/market-data")
    void getMarketData(@RequestParam int conid) {
        contractManagerService.getMarketData(conid);
    }
}
