package com.example.movie.service.pricing;

import java.math.BigDecimal;

public class VipSeatCalculator implements SeatPriceCalculator {
    @Override
    public BigDecimal calculate(BigDecimal basePrice) {
        return basePrice.add(BigDecimal.valueOf(20000));
    }
}