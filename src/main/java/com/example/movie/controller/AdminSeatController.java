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
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
@Controller @RequestMapping("/admin/seats")
public class AdminSeatController {
 private final SeatService seats;private final RoomService rooms;
 public AdminSeatController(SeatService seats,RoomService rooms){this.seats=seats;this.rooms=rooms;}
 @GetMapping public String list(@RequestParam Long roomId,Model m){
  m.addAttribute("room",rooms.getSummary(roomId));var layout=seats.loadLayout(roomId);var all=layout.seats();m.addAttribute("seats",all);
  m.addAttribute("layoutSnapshot",layout.snapshot());
  m.addAttribute("rows",all.stream().collect(Collectors.groupingBy(SeatSummary::rowLabel,LinkedHashMap::new,Collectors.toList())));
  m.addAttribute("capacity",rooms.capacity(roomId));return "admin/seats/list";
 }
 @PostMapping(value="/designer",consumes="application/json")
 @ResponseBody
 public org.springframework.http.ResponseEntity<?> saveDesigner(@Valid @RequestBody SeatLayoutForm form){
  try{return org.springframework.http.ResponseEntity.ok(seats.saveLayout(form));}
  catch(ScheduleValidationException ex){
   return org.springframework.http.ResponseEntity.status(409).body(java.util.Map.of("message",ex.getMessage(),"field",ex.getField()));
  }catch(org.springframework.dao.DataIntegrityViolationException ex){
   return org.springframework.http.ResponseEntity.status(409).body(java.util.Map.of("message","Vị trí ghế bị trùng hoặc đang có thay đổi đồng thời. Chưa lưu sơ đồ."));
  }
 }
 @GetMapping("/new") public String form(@RequestParam Long roomId,Model m){rooms.getSummary(roomId);m.addAttribute("form",new SeatForm(roomId,"A",1,SeatType.NORMAL,true));return prepare(null,m);}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m){var s=seats.getById(id);m.addAttribute("form",new SeatForm(s.roomId(),s.rowLabel(),s.seatNumber(),s.seatType(),s.active()));return prepare(id,m);}
 @PostMapping public String create(@Valid @ModelAttribute("form") SeatForm f,BindingResult e,Model m,RedirectAttributes r){return save(null,f,e,m,r);}
 @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") SeatForm f,BindingResult e,Model m,RedirectAttributes r){return save(id,f,e,m,r);}
 @GetMapping("/batch") public String batchForm(@RequestParam Long roomId,Model m){
  var form=new SeatBatchForm();form.setRoomId(roomId);
  form.getRows().add(new SeatRowForm("A",1,12,SeatType.NORMAL,true));
  m.addAttribute("form",form);return prepareBatch(roomId,m);
 }
 @PostMapping("/batch/preview") public String preview(@Valid @ModelAttribute("form") SeatBatchForm f,BindingResult e,Model m){
  ScheduleControllerSupport.save(e,()->{
   var preview=seats.previewBatch(f);
   m.addAttribute("previewRows",preview.stream().collect(Collectors.groupingBy(SeatSummary::rowLabel,LinkedHashMap::new,Collectors.toList())));
   m.addAttribute("previewCount",preview.stream().filter(s->s.id()==null).count());
  });
  return prepareBatch(f.getRoomId(),m);
 }
 @PostMapping("/batch") public String batchCreate(@Valid @ModelAttribute("form") SeatBatchForm f,BindingResult e,Model m,RedirectAttributes r){
  if(!ScheduleControllerSupport.save(e,()->{
   int count=seats.createBatch(f).size();r.addFlashAttribute("success","Đã tạo "+count+" ghế. Ghế cũ được giữ nguyên.");
  }))return prepareBatch(f.getRoomId(),m);
  return "redirect:/admin/seats?roomId="+f.getRoomId();
 }
 @PostMapping("/batch-update") public String batchUpdate(@Valid @ModelAttribute("bulkForm") SeatBulkUpdateForm f,BindingResult e,Model m,RedirectAttributes r){
  if(!ScheduleControllerSupport.save(e,()->{
   int count=seats.updateBatch(f);r.addFlashAttribute("success","Đã cập nhật "+count+" ghế, giữ nguyên ID và vị trí.");
  })){
   r.addFlashAttribute("failure",e.getAllErrors().stream().map(x->
    x.getCodes()!=null&&java.util.Arrays.stream(x.getCodes()).anyMatch(c->c.startsWith("typeMismatch"))
     ?"Lựa chọn loại ghế, trạng thái hoặc ID không hợp lệ.":x.getDefaultMessage()).collect(Collectors.joining(" ")));
  }
  return f.roomId()==null?"redirect:/admin/rooms":"redirect:/admin/seats?roomId="+f.roomId();
 }
 private String prepareBatch(Long roomId,Model m){
  if(roomId==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Vui lòng chọn phòng.");
  m.addAttribute("room",rooms.getSummary(roomId));return "admin/seats/batch";
 }
 private String save(Long id,SeatForm f,BindingResult e,Model m,RedirectAttributes r){
  if(!ScheduleControllerSupport.save(e,()->{if(id==null)seats.create(f);else seats.update(id,f);}))return prepare(id,m);
  r.addFlashAttribute("success","Đã lưu ghế, giữ nguyên ID khi chỉnh sửa.");return "redirect:/admin/seats?roomId="+f.roomId();
 }
 private String prepare(Long id,Model m){m.addAttribute("id",id);m.addAttribute("rooms",rooms.findForAdmin(null));return "admin/seats/form";}
}
