package com.example.tradeservice.controller;

import com.example.tradeservice.model.OrderModel;
import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.service.impl.PositionTracker;
import com.ib.client.Contract;
import com.ib.client.Types;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collection;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    PositionTracker positionTracker;
    OrderTracker orderTracker;

    @PostMapping
    void placeOrder(@RequestParam String symbol, @RequestParam String action,
                    @RequestParam BigDecimal quantity,
                    @RequestParam BigDecimal price) {
        Contract contract = positionTracker.getPositionBySymbol(symbol).getContract();

        orderTracker.placeMarketOrder(contract, Types.Action.BUY, 10, price, price.add(BigDecimal.ONE));
    }

    @GetMapping
    Collection<OrderModel> getAllOrders() {
        return orderTracker.getAllOrders();
    }
}
