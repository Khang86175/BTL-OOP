package com.example.movie.controller;
import com.example.movie.dto.*;
import com.example.movie.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/admin/theaters")
public class AdminTheaterController {
 private final TheaterService theaters;
 public AdminTheaterController(TheaterService theaters){this.theaters=theaters;}
 @GetMapping public String list(Model m){m.addAttribute("theaters",theaters.findForAdmin());return "admin/theaters/list";}
 @GetMapping("/new") public String form(Model m){m.addAttribute("form",new TheaterForm("","",true));return prepare(null,m);}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m){
  var t=theaters.getById(id);m.addAttribute("form",new TheaterForm(t.getName(),t.getAddress(),t.isActive()));return prepare(id,m);
 }
 @PostMapping public String create(@Valid @ModelAttribute("form") TheaterForm f,BindingResult e,Model m,RedirectAttributes r){return save(null,f,e,m,r);}
 @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") TheaterForm f,BindingResult e,Model m,RedirectAttributes r){return save(id,f,e,m,r);}
 private String save(Long id,TheaterForm f,BindingResult e,Model m,RedirectAttributes r){
  if(!ScheduleControllerSupport.save(e,()->{if(id==null)theaters.create(f);else theaters.update(id,f);}))return prepare(id,m);
  r.addFlashAttribute("success","Đã lưu rạp.");return "redirect:/admin/theaters";
 }
 private String prepare(Long id,Model m){m.addAttribute("id",id);return "admin/theaters/form";}
 @PostMapping("/{id}/active") public String active(@PathVariable Long id,@RequestParam boolean active,RedirectAttributes r){theaters.setActive(id,active);r.addFlashAttribute("success","Đã cập nhật hoạt động rạp.");return "redirect:/admin/theaters";}
}
