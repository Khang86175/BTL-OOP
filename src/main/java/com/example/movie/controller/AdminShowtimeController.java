package com.example.movie.controller;
import com.example.movie.dto.*;
import com.example.movie.model.*;
import com.example.movie.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/admin/showtimes")
public class AdminShowtimeController {
 private final ShowtimeService schedules;private final RoomService rooms;private final MovieService movies;
 public AdminShowtimeController(ShowtimeService schedules,RoomService rooms,MovieService movies){this.schedules=schedules;this.rooms=rooms;this.movies=movies;}
 @GetMapping public String list(Model m){m.addAttribute("showtimes",schedules.findForAdmin());return "admin/showtimes/list";}
 @GetMapping("/new") public String form(Model m){m.addAttribute("form",new ShowtimeForm(null,null,null,null,ShowtimeStatus.OPEN));return prepare(null,m);}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m){
  var s=schedules.getForAdmin(id);m.addAttribute("form",new ShowtimeForm(s.movieId(),s.roomId(),s.startTime(),s.basePrice(),s.status()));return prepare(id,m);
 }
 @PostMapping public String create(@Valid @ModelAttribute("form") ShowtimeForm f,BindingResult e,Model m,RedirectAttributes r){return save(null,f,e,m,r);}
 @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") ShowtimeForm f,BindingResult e,Model m,RedirectAttributes r){return save(id,f,e,m,r);}
 private String save(Long id,ShowtimeForm f,BindingResult e,Model m,RedirectAttributes r){
  if(!ScheduleControllerSupport.save(e,()->{if(id==null)schedules.create(f);else schedules.update(id,f);}))return prepare(id,m);
  r.addFlashAttribute("success","Đã lưu lịch chiếu.");return "redirect:/admin/showtimes";
 }
 private String prepare(Long id,Model m){
  m.addAttribute("id",id);m.addAttribute("movies",movies.findForAdmin(null,null,null,"title"));m.addAttribute("rooms",rooms.findForAdmin(null));return "admin/showtimes/form";
 }
 @PostMapping("/{id}/status") public String status(@PathVariable Long id,@RequestParam ShowtimeStatus status,RedirectAttributes r){
  try{schedules.changeStatus(id,status);r.addFlashAttribute("success","Đã cập nhật trạng thái lịch.");}
  catch(ScheduleValidationException ex){r.addFlashAttribute("failure",ex.getMessage());}
  return "redirect:/admin/showtimes";
 }
}
