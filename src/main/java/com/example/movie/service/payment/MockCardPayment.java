package com.example.movie.service.payment;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Component("CARD")
public class MockCardPayment implements PaymentStrategy {
    @Override
    public PaymentResult pay(BigDecimal amount) {
        return new PaymentResult(true, "MOCK-CARD-" + UUID.randomUUID());
    }
}
