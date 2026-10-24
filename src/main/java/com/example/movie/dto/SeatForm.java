package com.example.movie.dto;
import com.example.movie.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
public record SeatForm(@NotNull(message="Vui lòng chọn phòng.") @Positive(message="Phòng không hợp lệ.") Long roomId, @NotBlank(message="Vui lòng nhập hàng ghế.") @Size(max=10,message="Hàng ghế tối đa 10 ký tự.") String rowLabel, @NotNull(message="Vui lòng nhập số ghế.") @Min(value=1,message="Số ghế phải từ 1.") Integer seatNumber, @NotNull(message="Vui lòng chọn loại ghế.") SeatType seatType, boolean active) {}
