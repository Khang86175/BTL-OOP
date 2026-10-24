package com.example.movie.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

public class SeatBatchForm {
 @NotNull(message="Vui lòng chọn phòng.") @Positive(message="Phòng không hợp lệ.")
 private Long roomId;
 @NotEmpty(message="Cần ít nhất một hàng ghế.")
 @Size(max=30,message="Mỗi lần cấu hình tối đa 30 khoảng ghế.")
 private List<@NotNull(message="Hàng ghế không hợp lệ.") @Valid SeatRowForm> rows = new ArrayList<>();
 public Long getRoomId(){return roomId;}
 public void setRoomId(Long roomId){this.roomId=roomId;}
 public List<SeatRowForm> getRows(){return rows;}
 public void setRows(List<SeatRowForm> rows){this.rows=rows;}
}
