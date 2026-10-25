package com.example.movie.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingResponse {
    private Long bookingId;
    private Long showtimeId;
    private String userId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private List<String> seatNames;

    public BookingResponse(Long bookingId, Long showtimeId, String userId, BigDecimal totalAmount, String status, LocalDateTime createdAt, List<String> seatNames) {
        this.bookingId = bookingId;
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.seatNames = seatNames;
    }

    // Tự sinh Getters bằng Alt + Insert nếu cần
    public Long getBookingId() { return bookingId; }
    public Long getShowtimeId() { return showtimeId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<String> getSeatNames() { return seatNames; }
}