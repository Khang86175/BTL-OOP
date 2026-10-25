package com.example.movie.controller;
import com.example.movie.service.ScheduleValidationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
@ControllerAdvice(assignableTypes={AdminTheaterController.class,AdminRoomController.class,AdminSeatController.class,AdminShowtimeController.class,ShowtimeController.class})
public class ScheduleWebAdvice {
 @ModelAttribute public void labels(Model m){
  m.addAttribute("roomLabels",Map.of("TWO_D","2D","THREE_D","3D","IMAX","IMAX"));
  m.addAttribute("seatLabels",Map.of("NORMAL","Thường","VIP","VIP"));
  m.addAttribute("statusLabels",Map.of("OPEN","Mở bán","CLOSED","Đã đóng","CANCELLED","Đã hủy"));
 }
 @ExceptionHandler(ScheduleValidationException.class)
 public String notFound(ScheduleValidationException ex,HttpServletResponse response){
  response.setStatus(404);return "error/404";
 }
}
