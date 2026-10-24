package com.example.movie.dto;
import com.example.movie.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
public record TheaterForm(@NotBlank(message="Vui lòng nhập tên rạp.") @Size(max=150,message="Tên rạp tối đa 150 ký tự.") String name, @NotBlank(message="Vui lòng nhập địa chỉ.") @Size(max=500,message="Địa chỉ tối đa 500 ký tự.") String address, boolean active) {}
