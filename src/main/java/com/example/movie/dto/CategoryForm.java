package com.example.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryForm {
    @NotBlank(message = "Vui lòng nhập tên thể loại.")
    @Size(max = 80, message = "Tên thể loại tối đa 80 ký tự.")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
