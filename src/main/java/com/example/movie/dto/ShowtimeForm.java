package com.example.movie.dto;
import com.example.movie.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
public record ShowtimeForm(@NotNull(message="Vui lòng chọn phim.") @Positive(message="Phim không hợp lệ.") Long movieId, @NotNull(message="Vui lòng chọn phòng.") @Positive(message="Phòng không hợp lệ.") Long roomId, @NotNull(message="Vui lòng nhập giờ bắt đầu.") @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime, @NotNull(message="Vui lòng nhập giá cơ bản.") @DecimalMin(value="0",message="Giá không được âm.") @Digits(integer=10,fraction=2,message="Giá tối đa 10 chữ số nguyên và 2 chữ số thập phân.") BigDecimal basePrice, @NotNull(message="Vui lòng chọn trạng thái.") ShowtimeStatus status) {}
