package com.example.movie.dto;
import java.util.List;
public record SeatLayoutState(List<SeatSummary> seats,String snapshot) {}
