package com.example.movie.controller;

import com.example.movie.dto.*;
import com.example.movie.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Controller @RequestMapping("/admin/seats")
public class AdminSeatController {
 private final SeatService seats;
 private final RoomService rooms;
 public AdminSeatController(SeatService seats,RoomService rooms){this.seats=seats;this.rooms=rooms;}

 @GetMapping public String list(@RequestParam Long roomId,Model model){
  return prepareList(roomId,model);
 }
 @PostMapping("/layout")
 public String createLayout(@Valid @ModelAttribute("createForm") SeatLayoutCreateForm form,BindingResult errors,Model model,RedirectAttributes redirect){
  if(!ScheduleControllerSupport.save(errors,()->seats.saveLayout(form))){
   model.addAttribute("failure",errors.getAllErrors().stream().map(error->
    error instanceof org.springframework.validation.FieldError field&&field.isBindingFailure()
     ?"Số hàng và số ghế phải là số nguyên hợp lệ.":error.getDefaultMessage()).collect(Collectors.joining(" ")));
   Long roomId=form==null?null:form.roomId();
   if(roomId==null){
    var raw=errors.getFieldValue("roomId");
    if(raw instanceof Long value)roomId=value;
    else if(raw!=null)try{roomId=Long.valueOf(raw.toString());}catch(NumberFormatException ignored){}
   }
   return prepareList(roomId,model);
  }
  redirect.addFlashAttribute("success","Đã lưu sơ đồ. Ghế giữ nguyên vị trí được giữ ID, loại và trạng thái.");
  return "redirect:/admin/seats?roomId="+form.roomId();
 }
 @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model model){
  var seat=seats.getById(id);
  model.addAttribute("form",new SeatForm(seat.roomId(),seat.rowLabel(),seat.seatNumber(),seat.seatType(),seat.active()));
  return prepareEdit(id,seat.roomId(),model);
 }
 @PostMapping("/{id}")
 public String update(@PathVariable Long id,@Valid @ModelAttribute("form") SeatForm form,BindingResult errors,Model model,RedirectAttributes redirect){
  if(!ScheduleControllerSupport.save(errors,()->seats.update(id,form))){
   var seat=seats.getById(id);
   if(form==null){
    var fallback=new SeatForm(seat.roomId(),seat.rowLabel(),seat.seatNumber(),seat.seatType(),seat.active());
    var safeErrors=new org.springframework.validation.BeanPropertyBindingResult(fallback,"form");
    safeErrors.reject("seat.invalid","Giá trị không đúng định dạng. Hãy kiểm tra loại ghế và trạng thái.");
    model.addAttribute("form",fallback);model.addAttribute(BindingResult.MODEL_KEY_PREFIX+"form",safeErrors);
   }
   return prepareEdit(id,seat.roomId(),model);
  }
  redirect.addFlashAttribute("success","Đã cập nhật ghế, giữ nguyên ID và vị trí.");
  return "redirect:/admin/seats?roomId="+form.roomId();
 }
 @PostMapping("/{id}/delete")
 public String deleteSeat(@PathVariable Long id,@RequestParam Long roomId,@RequestParam(defaultValue="false") boolean confirmed,RedirectAttributes redirect){
  return remove(roomId,redirect,()->seats.deleteSeat(roomId,id,confirmed));
 }
 @PostMapping("/layout/delete")
 public String deleteLayout(@RequestParam Long roomId,@RequestParam(defaultValue="false") boolean confirmed,RedirectAttributes redirect){
  return remove(roomId,redirect,()->seats.deleteLayout(roomId,confirmed));
 }
 private String remove(Long roomId,RedirectAttributes redirect,Runnable action){
  try{action.run();redirect.addFlashAttribute("success","Đã xóa ghế chưa được tham chiếu.");}
  catch(ScheduleValidationException ex){redirect.addFlashAttribute("failure",ex.getMessage());}
  catch(org.springframework.dao.DataIntegrityViolationException ex){redirect.addFlashAttribute("failure","Ghế đang được tham chiếu hoặc có thay đổi đồng thời. Chưa xóa dữ liệu.");}
  return "redirect:/admin/seats?roomId="+roomId;
 }
 private String prepareList(Long roomId,Model model){
  if(roomId==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Thiếu phòng.");
  model.addAttribute("room",rooms.getSummary(roomId));
  var all=seats.findByRoom(roomId);model.addAttribute("seats",all);
  if(!model.containsAttribute("createForm")){
   int rowCount=all.stream().mapToInt(s->rowOrdinal(s.rowLabel())).max().orElse(5);
   int count=all.stream().mapToInt(SeatSummary::seatNumber).max().orElse(8);
   model.addAttribute("createForm",new SeatLayoutCreateForm(roomId,Math.min(30,Math.max(1,rowCount)),Math.min(200,Math.max(1,count))));
  }
  model.addAttribute("rows",all.stream().collect(Collectors.groupingBy(SeatSummary::rowLabel,LinkedHashMap::new,Collectors.toList())));
  model.addAttribute("hasLegacySeatTypes",all.stream().anyMatch(s->!s.seatType().isSupported()));
  return "admin/seats/list";
 }
 private String prepareEdit(Long id,Long roomId,Model model){
  model.addAttribute("id",id);model.addAttribute("room",rooms.getSummary(roomId));
  model.addAttribute("seat",seats.getById(id));
  return "admin/seats/form";
 }
 private static int rowOrdinal(String row){
  String label=row.strip().toUpperCase(java.util.Locale.ROOT);if(!label.matches("[A-Z]{1,3}"))return 1;
  int value=0;for(char c:label.toCharArray())value=value*26+c-'A'+1;return value;
 }
}
