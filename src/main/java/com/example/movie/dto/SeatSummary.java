package com.example.movie.dto;
import com.example.movie.model.*;
public record SeatSummary(Long id, Long roomId, String rowLabel, Integer seatNumber, SeatType seatType, boolean active) {
 public static SeatSummary from(Seat s){return new SeatSummary(s.getId(),s.getRoom().getId(),s.getRowLabel(),s.getSeatNumber(),s.getSeatType(),s.isActive());}
}
