package com.example.movie.model;
public enum ShowtimeStatus {
 OPEN {
  @Override public boolean occupiesRoom() { return true; }
 },
 CLOSED {
  @Override public boolean occupiesRoom() { return true; }
 },
 CANCELLED {
  @Override public boolean occupiesRoom() { return false; }
 };

 public abstract boolean occupiesRoom();
}
