package com.example.movie.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** Complete desired layout. Omitted persisted seats are deactivated, never deleted. */
public record SeatLayoutForm(
 @NotNull @Positive Long roomId,
 @NotBlank(message="Thiếu phiên bản sơ đồ. Hãy tải lại trang.") String snapshot,
 @NotNull @Size(max=1000,message="Trình thiết kế hỗ trợ tối đa 1.000 ghế mỗi phòng.")
 List<@NotNull @Valid SeatLayoutSeatForm> seats,
 boolean confirmExistingChanges) {}
