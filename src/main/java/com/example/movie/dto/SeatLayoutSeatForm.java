package com.example.movie.dto;

import com.example.movie.model.SeatType;
import jakarta.validation.constraints.*;

/** Existing IDs are preserved; null ID identifies a requested new position. */
public record SeatLayoutSeatForm(
 @Positive Long id,
 @NotBlank(message="Hàng ghế không được trống.") @Size(max=10,message="Tên hàng tối đa 10 ký tự.") String rowLabel,
 @NotNull(message="Thiếu số ghế.") @Min(value=1,message="Số ghế phải từ 1.") Integer seatNumber,
 @NotNull(message="Thiếu loại ghế.") SeatType seatType,
 boolean active) {}
