package com.example.movie.controller;
import com.example.movie.model.Movie;
import com.example.movie.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
@Controller @RequestMapping("/showtimes")
public class ShowtimeController {
 private final ShowtimeService schedules;private final TheaterService theaters;private final MovieService movies;
 public ShowtimeController(ShowtimeService schedules,TheaterService theaters,MovieService movies){this.schedules=schedules;this.theaters=theaters;this.movies=movies;}
 @GetMapping public String list(@RequestParam(required=false) Long movieId,@RequestParam(required=false) Long theaterId,
  @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,Model m){
  LocalDate day=date==null?LocalDate.now(Movie.CINEMA_ZONE):date;
  m.addAttribute("showtimes",schedules.findOpenShowtimes(movieId,theaterId,day));m.addAttribute("movies",movies.findVisible(null,null,null,"title"));
  m.addAttribute("theaters",theaters.findActive());m.addAttribute("movieId",movieId);m.addAttribute("theaterId",theaterId);m.addAttribute("date",day);return "showtimes/list";
 }
 @GetMapping("/{id}") public String detail(@PathVariable Long id,Model m){
  m.addAttribute("showtime",schedules.getBookableShowtime(id));return "showtimes/detail";
 }
}
