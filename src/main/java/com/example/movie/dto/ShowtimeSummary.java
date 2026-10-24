package com.example.movie.dto;
import com.example.movie.model.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
/** Times are Asia/Ho_Chi_Minh; basePrice is VND including room pricing, excluding seat surcharge. */
public record ShowtimeSummary(Long id, Long movieId, String movieTitle, Long theaterId, String theaterName,
 String theaterAddress, Long roomId, String roomName, RoomType roomType, LocalDateTime startTime,
 LocalDateTime endTime, BigDecimal basePrice, ShowtimeStatus status) {
 public static ShowtimeSummary from(Showtime s){
  Room r=s.getRoom(); Theater t=r.getTheater();
  return new ShowtimeSummary(s.getId(),s.getMovie().getId(),s.getMovie().getTitle(),t.getId(),t.getName(),
   t.getAddress(),r.getId(),r.getName(),r.getRoomType(),s.getStartTime(),s.getEndTime(),s.getBasePrice(),s.getStatus());
 }
}
