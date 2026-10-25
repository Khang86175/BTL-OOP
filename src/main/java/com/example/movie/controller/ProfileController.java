package com.example.movie.controller;

import com.example.movie.dto.UserProfileDto;
import com.example.movie.model.Role;
import com.example.movie.model.User;
import com.example.movie.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.format.DateTimeFormatter;

@Controller
public class ProfileController {
    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản đang đăng nhập."));

        String phone = user.getPhone();
        if (phone == null || phone.isBlank()) {
            phone = "Chưa cập nhật";
        }

        String createdAt = user.getCreatedAt() == null
                ? "Không có thông tin"
                : user.getCreatedAt().format(CREATED_AT_FORMAT);

        model.addAttribute("profile", new UserProfileDto(
                user.getFullName(), user.getEmail(), phone, displayRole(user.getRole()), createdAt));
        return "profile";
    }

    private String displayRole(Role role) {
        return role == Role.ADMIN ? "Quản trị viên" : "Khách hàng";
    }
}
