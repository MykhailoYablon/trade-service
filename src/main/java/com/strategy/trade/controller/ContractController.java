package com.strategy.trade.controller;

import com.strategy.trade.model.ContractModel;
import com.strategy.trade.service.ContractManagerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
