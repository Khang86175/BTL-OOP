package com.example.movie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterDto {
    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    private String password;

    @Pattern(regexp = "^(?:[0-9]{10,11})?$", message = "Số điện thoại phải gồm 10 hoặc 11 chữ số.")
    private String phone;

    // Getters và Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
