package com.example.tradeservice.controller;

import com.example.tradeservice.service.ContractManagerService;
import com.example.tradeservice.service.OrderTracker;
import com.ib.client.Contract;
import com.ib.client.Types;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    ContractManagerService contractManagerService;
    OrderTracker orderTracker;

    @PostMapping
    void placeOrder(@RequestParam int conid, @RequestParam String action, @RequestParam BigDecimal quantity,
                    @RequestParam double price) {
        Contract contract = contractManagerService.getContractHolder(conid).getContract();
        orderTracker.placeLimitOrder(contract, Types.Action.valueOf(action), quantity, price);
    }
}
