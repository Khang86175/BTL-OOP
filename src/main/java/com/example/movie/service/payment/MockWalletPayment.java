package com.example.movie.service.payment;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Component("WALLET")
public class MockWalletPayment implements PaymentStrategy {
    @Override
    public PaymentResult pay(BigDecimal amount) {
        return new PaymentResult(true, "MOCK-WALLET-" + UUID.randomUUID());
    }
}
