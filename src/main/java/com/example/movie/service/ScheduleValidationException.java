package com.example.movie.service;
public class ScheduleValidationException extends RuntimeException {
 private final String field;
 public ScheduleValidationException(String field,String message){super(message);this.field=field;}
 public String getField(){return field;}
}
