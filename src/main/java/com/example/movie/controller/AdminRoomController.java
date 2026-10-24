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
@Controller @RequestMapping("/admin/rooms")
public class AdminRoomController {
 private final RoomService rooms;private final TheaterService theaters;
 public AdminRoomController(RoomService rooms,TheaterService theaters){this.rooms=rooms;this.theaters=theaters;}
 @GetMapping public String list(@RequestParam(required=false) Long theaterId,Model m){
  m.addAttribute("rooms",rooms.findForAdmin(theaterId));m.addAttribute("theaters",theaters.findForAdmin());m.addAttribute("theaterId",theaterId);return "admin/rooms/list";
 }
 @GetMapping("/new") public String form(@RequestParam(required=false) Long theaterId,Model m){m.addAttribute("form",new RoomForm(theaterId,"",RoomType.TWO_D,true));return prepare(null,m);}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m){
  var s=rooms.getSummary(id);m.addAttribute("form",new RoomForm(s.theaterId(),s.name(),s.roomType(),s.active()));return prepare(id,m);
 }
 @PostMapping public String create(@Valid @ModelAttribute("form") RoomForm f,BindingResult e,Model m,RedirectAttributes r){return save(null,f,e,m,r);}
 @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") RoomForm f,BindingResult e,Model m,RedirectAttributes r){return save(id,f,e,m,r);}
 private String save(Long id,RoomForm f,BindingResult e,Model m,RedirectAttributes r){
  if(!ScheduleControllerSupport.save(e,()->{if(id==null)rooms.create(f);else rooms.update(id,f);}))return prepare(id,m);
  r.addFlashAttribute("success","Đã lưu phòng.");return "redirect:/admin/rooms?theaterId="+f.theaterId();
 }
 private String prepare(Long id,Model m){m.addAttribute("id",id);m.addAttribute("theaters",theaters.findForAdmin());return "admin/rooms/form";}
 @PostMapping("/{id}/active") public String active(@PathVariable Long id,@RequestParam boolean active,RedirectAttributes r){rooms.setActive(id,active);r.addFlashAttribute("success","Đã cập nhật hoạt động phòng.");return "redirect:/admin/rooms";}
}
