package com.example.movie.dto;

import jakarta.validation.constraints.*;

/** Requested rectangle; removal of persisted seats requires explicit confirmation. */
public record SeatLayoutCreateForm(
 @NotNull @Positive Long roomId,
 @Min(value=1,message="Số hàng phải từ 1 đến 30.") @Max(value=30,message="Số hàng phải từ 1 đến 30.") int rowCount,
 @Min(value=1,message="Số ghế mỗi hàng phải từ 1 đến 200.") @Max(value=200,message="Số ghế mỗi hàng phải từ 1 đến 200.") int seatsPerRow,
 Boolean confirmRemoval) {
 public SeatLayoutCreateForm(Long roomId,int rowCount,int seatsPerRow){this(roomId,rowCount,seatsPerRow,false);}
}
