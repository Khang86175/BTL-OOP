package com.example.movie.service.pricing;

import java.math.BigDecimal;

public interface SeatPriceCalculator {
    BigDecimal calculate(BigDecimal basePrice);
}