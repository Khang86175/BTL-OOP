package com.example.movie.service;
import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Validated @Transactional(readOnly=true)
public class ShowtimeService {
 private final ShowtimeRepository showtimes; private final RoomRepository rooms;
 private final SeatRepository seats; private final MovieService movies;
 public ShowtimeService(ShowtimeRepository showtimes,RoomRepository rooms,SeatRepository seats,MovieService movies){
  this.showtimes=showtimes;this.rooms=rooms;this.seats=seats;this.movies=movies;
 }
 @Transactional public ShowtimeSummary create(@NotNull @Valid ShowtimeForm f){ScheduleAccess.admin();return save(null,f);}
 @Transactional public ShowtimeSummary update(Long id,@NotNull @Valid ShowtimeForm f){ScheduleAccess.admin();return save(id,f);}
 public List<ShowtimeSummary> findForAdmin(){
  ScheduleAccess.admin();
  return showtimes.findAll(org.springframework.data.domain.Sort.by("startTime","id")).stream().map(ShowtimeSummary::from).toList();
 }
 public ShowtimeSummary getForAdmin(Long id){
  ScheduleAccess.admin();
  return ShowtimeSummary.from(showtimes.findById(id).orElseThrow(()->new ScheduleValidationException("showtimeId","Không tìm thấy lịch.")));
 }
 @Transactional public void changeStatus(Long id,@NotNull ShowtimeStatus status){
  ScheduleAccess.admin();
  if(status!=ShowtimeStatus.CLOSED&&status!=ShowtimeStatus.CANCELLED)
   throw new ScheduleValidationException("status","Chỉ có thể đóng hoặc hủy qua thao tác này.");
  Long roomId=showtimes.findRoomIdById(id).orElseThrow(()->new ScheduleValidationException("showtimeId","Không tìm thấy lịch."));
  rooms.findLockedById(roomId).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  var s=showtimes.findLockedById(id).orElseThrow(()->new ScheduleValidationException("showtimeId","Không tìm thấy lịch."));
  if(!s.getRoom().getId().equals(roomId))
   throw new ScheduleValidationException("roomId","Lịch vừa đổi phòng. Vui lòng tải lại và thử lại.");
  if(s.getStatus()==ShowtimeStatus.CANCELLED&&status==ShowtimeStatus.CLOSED)
   throw new ScheduleValidationException("status","Lịch đã hủy không thể đóng lại; hãy sửa lịch để kiểm tra xung đột.");
  s.setStatus(status);
 }
 private ShowtimeSummary save(Long id,ShowtimeForm f){
  // Serialize all writes per room, including when that room has no existing showtimes.
  Room r=rooms.findLockedById(f.roomId()).orElseThrow(()->new ScheduleValidationException("roomId","Không tìm thấy phòng."));
  Showtime s=id==null?new Showtime():showtimes.findLockedById(id).orElseThrow(()->new ScheduleValidationException("showtimeId","Không tìm thấy lịch."));
  if(!r.isOperational())throw new ScheduleValidationException("roomId","Phòng hoặc rạp ngừng hoạt động.");
  Movie m=movies.getVisibleMovie(f.movieId());
  if(f.startTime().isBefore(LocalDateTime.now(Movie.CINEMA_ZONE)))throw new ScheduleValidationException("startTime","Không lập lịch trong quá khứ.");
  if(f.startTime().toLocalDate().isBefore(m.getReleaseDate()))throw new ScheduleValidationException("startTime","Lịch phải từ ngày khởi chiếu.");
  LocalDateTime end=f.startTime().plusMinutes(m.getDurationMinutes());
  if(f.status().occupiesRoom()&&showtimes.countOverlaps(r.getId(),id==null?-1L:id,f.startTime(),end)>0)
   throw new ScheduleValidationException("startTime","Lịch chiếu trùng trong phòng.");
  s.setMovie(m);s.setRoom(r);s.setStartTime(f.startTime());s.setEndTime(end);
  if(id==null)s.setBasePrice(f.normalPrice()); // Satisfy the legacy NOT NULL column; never treat it as the new VIP price.
  s.setNormalPrice(f.normalPrice());s.setVipPrice(f.vipPrice());s.setStatus(f.status());
  return ShowtimeSummary.from(showtimes.saveAndFlush(s));
 }
 public List<ShowtimeSummary> findOpenShowtimes(Long movieId,Long theaterId,LocalDate date){
  LocalDate day=date==null?LocalDate.now(Movie.CINEMA_ZONE):date;
  LocalDateTime now=LocalDateTime.now(Movie.CINEMA_ZONE);
  return showtimes.findSchedule(movieId,theaterId,day.atStartOfDay(),day.plusDays(1).atStartOfDay())
   .stream().filter(s->s.isBookableAt(now)).map(ShowtimeSummary::from).toList();
 }
 public ShowtimeSummary getBookableShowtime(Long id){return ShowtimeSummary.from(requireBookable(id));}
 public List<SeatSummary> findActiveSeats(Long showtimeId){
  Showtime s=requireBookable(showtimeId);
  return seats.findByRoomIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(s.getRoom().getId()).stream()
   .filter(seat->seat.getSeatType().isSupported()).map(SeatSummary::from).toList();
 }
 private Showtime requireBookable(Long id){
  Showtime s=showtimes.findById(id).orElseThrow(()->new ScheduleValidationException("showtimeId","Không tìm thấy lịch."));
  if(!s.isBookableAt(LocalDateTime.now(Movie.CINEMA_ZONE)))throw new ScheduleValidationException("showtimeId","Lịch không mở bán.");
  return s;
 }
}
