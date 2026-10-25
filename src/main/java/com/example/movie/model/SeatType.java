package com.example.movie.model;
public enum SeatType {
 NORMAL, VIP,
 /** Read compatibility only. Migrate persisted values before removing this constant. */
 @Deprecated COUPLE;
 public boolean isSupported(){return this==NORMAL||this==VIP;}
}
