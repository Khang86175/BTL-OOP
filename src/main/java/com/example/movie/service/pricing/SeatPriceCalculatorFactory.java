package com.example.movie.service.pricing;

import com.example.movie.model.SeatType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class SeatPriceCalculatorFactory {

    private final Map<SeatType, SeatPriceCalculator> calculators = new EnumMap<>(SeatType.class);

    public SeatPriceCalculatorFactory() {
        calculators.put(SeatType.NORMAL, new RegularSeatCalculator());
        calculators.put(SeatType.VIP, new VipSeatCalculator());
        calculators.put(SeatType.COUPLE, new CoupleSeatCalculator());
    }

    public SeatPriceCalculator getCalculator(SeatType seatType) {
        return calculators.getOrDefault(seatType, new RegularSeatCalculator());
    }
}