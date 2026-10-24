package com.example.movie.controller;
import com.example.movie.service.ScheduleValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
final class ScheduleControllerSupport {
 private ScheduleControllerSupport(){}
 static boolean save(BindingResult errors,Runnable action){
  if(errors.hasErrors())return false;
  try{action.run();return true;}
  catch(ScheduleValidationException ex){errors.reject("schedule.invalid",ex.getMessage());}
  catch(DataIntegrityViolationException ex){errors.reject("schedule.duplicate","Dữ liệu trùng hoặc đang được tham chiếu. Vui lòng kiểm tra lại.");}
  catch(ResponseStatusException ex){if(ex.getStatusCode().value()!=404)throw ex;errors.reject("schedule.movie","Phim không tồn tại hoặc đã bị ẩn.");}
  return false;
 }
}
