package com.example.demo.it213_session11_bai1;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
class PaymentController {
    @GetMapping("/ping")
    Map<String, Object> ping() {
        return Map.of("status", "accepted", "timestamp", Instant.now().toString());
    }
}
