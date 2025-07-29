package com.example.tradeservice.controller;

import com.example.tradeservice.model.OrderHolder;
import com.example.tradeservice.model.OrderModel;
import com.example.tradeservice.service.OrderTracker;
import com.example.tradeservice.service.impl.PositionTracker;
import com.ib.client.Contract;
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
    void placeOrder(@RequestParam String conid, @RequestParam String action, @RequestParam BigDecimal quantity,
                    @RequestParam double price) {
        Contract contract = positionTracker.getPositionByConid(Integer.valueOf(conid)).getContract();
        orderTracker.placeLimitOrder(contract, action, quantity, price);
    }

    @GetMapping
    Collection<OrderModel> getAllOrders() {
        return orderTracker.getAllOrders();
    }
}
