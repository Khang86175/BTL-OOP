package com.example.movie.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        Long showtimeId,
        String username,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt,
        List<String> seatCodes
) {}