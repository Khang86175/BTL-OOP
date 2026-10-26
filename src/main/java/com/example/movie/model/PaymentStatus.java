package com.example.movie.model;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    /** Legacy value kept so old rows can be read before migration runs. */
    @Deprecated
    PAID
}
