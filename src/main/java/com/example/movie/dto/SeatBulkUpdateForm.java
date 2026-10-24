package com.example.movie.dto;

import com.example.movie.model.SeatType;
import jakarta.validation.constraints.*;
import java.util.List;

/** Null type/active means keep that property unchanged. No identity or position edits. */
public record SeatBulkUpdateForm(
 @NotNull(message="Vui lòng chọn phòng.") @Positive(message="Phòng không hợp lệ.") Long roomId,
 @NotEmpty(message="Hãy chọn ít nhất một ghế.") @Size(max=1000,message="Mỗi lần cập nhật tối đa 1.000 ghế.")
 List<@NotNull @Positive Long> seatIds,
 SeatType seatType, Boolean active) {}
