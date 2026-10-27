package com.example.movie.service.payment;

import java.math.BigDecimal;

/** Safe demo payment contract. It never receives card, wallet, OTP or password data. */
public interface PaymentStrategy {
    PaymentResult pay(BigDecimal amount);
}
