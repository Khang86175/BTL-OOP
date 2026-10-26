package com.example.movie.dto;

import java.math.BigDecimal;
import java.util.List;

public class BookingPreview {
    private final Long showtimeId;
    private final List<Long> seatIds;
    private final List<String> seatNames;
    private final BigDecimal totalAmount;

    public BookingPreview(Long showtimeId, List<Long> seatIds, List<String> seatNames, BigDecimal totalAmount) {
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
        this.seatNames = seatNames;
        this.totalAmount = totalAmount;
    }

    public Long getShowtimeId() { return showtimeId; }
    public List<Long> getSeatIds() { return seatIds; }
    public List<String> getSeatNames() { return seatNames; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
