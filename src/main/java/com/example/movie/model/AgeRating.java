package com.example.movie.model;

public enum AgeRating {
    P("P — Mọi độ tuổi"), K("K — Dưới 13 tuổi đi cùng người giám hộ"),
    T13("T13 — Từ 13 tuổi"), T16("T16 — Từ 16 tuổi"), T18("T18 — Từ 18 tuổi");

    private final String label;
    AgeRating(String label) { this.label = label; }
    public String getLabel() { return label; }
}
