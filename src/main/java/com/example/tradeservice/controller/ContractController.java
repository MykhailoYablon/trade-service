package com.example.tradeservice.controller;

import com.example.tradeservice.model.ContractModel;
import com.example.tradeservice.service.ContractManagerService;
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
