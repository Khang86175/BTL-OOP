package com.example.movie.dto;

import java.util.List;

public class BookingRequest {
    private Long userId;
    private Long showtimeId;
    private List<Long> seatIds;

    public BookingRequest() {
    }

    public BookingRequest(Long userId, Long showtimeId, List<Long> seatIds) {
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }
}