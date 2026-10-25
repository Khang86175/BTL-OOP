package com.example.movie.service.pricing;

import java.math.BigDecimal;

public class RegularSeatCalculator implements SeatPriceCalculator {
    @Override
    public BigDecimal calculate(BigDecimal basePrice) {
        return basePrice;
    }
}