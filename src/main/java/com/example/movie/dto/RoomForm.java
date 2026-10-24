package com.example.movie.dto;
import com.example.movie.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
public record RoomForm(@NotNull(message="Vui lòng chọn rạp.") @Positive(message="Rạp không hợp lệ.") Long theaterId, @NotBlank(message="Vui lòng nhập tên phòng.") @Size(max=80,message="Tên phòng tối đa 80 ký tự.") String name, @NotNull(message="Vui lòng chọn loại phòng.") RoomType roomType, boolean active) {}
