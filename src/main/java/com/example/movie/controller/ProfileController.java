package com.example.movie.controller;

import com.example.movie.dto.ChangePasswordDto;
import com.example.movie.dto.UserProfileDto;
import com.example.movie.model.Role;
import com.example.movie.model.User;
import com.example.movie.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.format.DateTimeFormatter;

@Controller
public class ProfileController {
    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());

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

    @GetMapping("/profile/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails principal) {
        if (bindingResult.hasErrors()) {
            return "change-password";
        }

        try {
            userService.changePassword(principal.getUsername(), dto);
            return "redirect:/profile?passwordChanged";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("passwordChange", e.getMessage());
            return "change-password";
        }
    }

    private String displayRole(Role role) {
        return role == Role.ADMIN ? "Quản trị viên" : "Khách hàng";
    }
}
