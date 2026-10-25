package com.example.movie.service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
/** Explicit service guard: does not require changing shared SecurityConfig. */
final class ScheduleAccess {
 private ScheduleAccess(){}
 static void admin(){
  var a=SecurityContextHolder.getContext().getAuthentication();
  if(a==null || !a.isAuthenticated() || a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("ROLE_ADMIN")))
   throw new AccessDeniedException("Chỉ Admin được quản lý rạp và lịch chiếu.");
 }
}
