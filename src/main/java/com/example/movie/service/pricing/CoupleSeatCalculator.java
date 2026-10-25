package com.example.movie.service.pricing;

import java.math.BigDecimal;

public class CoupleSeatCalculator implements SeatPriceCalculator {
    @Override
    public BigDecimal calculate(BigDecimal basePrice) {
        return basePrice.add(BigDecimal.valueOf(50000));
    }
}