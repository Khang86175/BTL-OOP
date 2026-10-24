package com.example.movie.config;

import com.example.movie.model.*;
import com.example.movie.repository.*;
import com.example.movie.service.MovieService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;

/** Opt-in demo only. Ready event runs after all CommandLineRunner catalog seeds. */
@Component
@Profile("demo")
@ConditionalOnProperty(name="showtime.seed.enabled",havingValue="true")
public class ShowtimeSeedData {
 private static final String NAME="CineTicket Demo • Rạp Người 3";
 private final TheaterRepository theaters;
 private final RoomRepository rooms;
 private final ShowtimeRepository showtimes;
 private final MovieService movies;

 public ShowtimeSeedData(TheaterRepository theaters,RoomRepository rooms,
  ShowtimeRepository showtimes,MovieService movies) {
  this.theaters=theaters;this.rooms=rooms;this.showtimes=showtimes;this.movies=movies;
 }

 @EventListener(ApplicationReadyEvent.class)
 @Transactional
 public void seed() {
  // Add the complete sample once. Never overwrite an administrator's subsequent edits.
  if(theaters.findAll().stream().anyMatch(t->NAME.equals(t.getName())))return;
  var choices=movies.findVisible(null,null,null,"title");
  if(choices.isEmpty())return;
  Theater t=new Theater();t.setName(NAME);t.setAddress("Khu trải nghiệm điện ảnh • dữ liệu giả lập");
  theaters.save(t);
  Room r=new Room();r.setTheater(t);r.setName("Phòng Demo");r.setRoomType(RoomType.TWO_D);
  rooms.save(r);
  // The demo room intentionally starts empty. Only the administrator designs its seats.
  Movie movie=choices.getFirst();
  LocalDate today=LocalDate.now(Movie.CINEMA_ZONE);
  LocalDate day=movie.getReleaseDate().isAfter(today)?movie.getReleaseDate():today;
  LocalDateTime start=day.atTime(18,0);
  if(!start.isAfter(LocalDateTime.now(Movie.CINEMA_ZONE)))start=start.plusDays(1);
  for(int i=0;i<2;i++) {
   Showtime s=new Showtime();s.setMovie(movie);s.setRoom(r);s.setStartTime(start);
   s.setEndTime(start.plusMinutes(movie.getDurationMinutes()));
   s.setBasePrice(new BigDecimal("90000"));s.setStatus(ShowtimeStatus.OPEN);showtimes.save(s);
   start=s.getEndTime().plusMinutes(30);
  }
 }
}
