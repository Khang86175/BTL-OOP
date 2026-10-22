package com.example.movie.model;

public enum ScreeningStatus {
    NOW_SHOWING("Đang chiếu"), COMING_SOON("Sắp chiếu");

    private final String label;
    ScreeningStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
