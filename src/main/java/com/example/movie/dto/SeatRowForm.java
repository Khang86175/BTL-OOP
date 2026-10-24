package com.example.movie.dto;

import com.example.movie.model.SeatType;
import jakarta.validation.constraints.*;

/** One row segment. Multiple non-overlapping segments can leave a real aisle. */
public class SeatRowForm {
 @NotBlank(message="Vui lòng nhập tên hàng.")
 @Size(max=10,message="Tên hàng tối đa 10 ký tự.")
 private String rowLabel;
 @NotNull(message="Vui lòng nhập số ghế bắt đầu.")
 @Min(value=1,message="Số ghế bắt đầu phải từ 1.")
 @Max(value=200,message="Số ghế bắt đầu tối đa 200.")
 private Integer firstNumber;
 @NotNull(message="Vui lòng nhập số ghế kết thúc.")
 @Min(value=1,message="Số ghế kết thúc phải từ 1.")
 @Max(value=200,message="Số ghế kết thúc tối đa 200.")
 private Integer lastNumber;
 @NotNull(message="Vui lòng chọn loại ghế.")
 private SeatType seatType = SeatType.NORMAL;
 private boolean active = true;

 public SeatRowForm() {}
 public SeatRowForm(String rowLabel,Integer firstNumber,Integer lastNumber,SeatType seatType,boolean active) {
  this.rowLabel=rowLabel;this.firstNumber=firstNumber;this.lastNumber=lastNumber;
  this.seatType=seatType;this.active=active;
 }
 public String getRowLabel(){return rowLabel;}
 public void setRowLabel(String rowLabel){this.rowLabel=rowLabel;}
 public Integer getFirstNumber(){return firstNumber;}
 public void setFirstNumber(Integer firstNumber){this.firstNumber=firstNumber;}
 public Integer getLastNumber(){return lastNumber;}
 public void setLastNumber(Integer lastNumber){this.lastNumber=lastNumber;}
 public SeatType getSeatType(){return seatType;}
 public void setSeatType(SeatType seatType){this.seatType=seatType;}
 public boolean isActive(){return active;}
 public void setActive(boolean active){this.active=active;}
}
