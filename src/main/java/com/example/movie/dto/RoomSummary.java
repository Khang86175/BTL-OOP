package com.example.movie.dto;
import com.example.movie.model.*;
public record RoomSummary(Long id, Long theaterId, String theaterName, String name,
 RoomType roomType, boolean active, boolean operational) {
 public static RoomSummary from(Room r) {
  return new RoomSummary(r.getId(),r.getTheater().getId(),r.getTheater().getName(),
   r.getName(),r.getRoomType(),r.isActive(),r.isOperational());
 }
}
