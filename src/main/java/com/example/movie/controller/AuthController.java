package com.example.movie.controller;

import com.example.movie.dto.RegisterDto;
import com.example.movie.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                                 BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Dữ liệu đăng ký chưa hợp lệ. Vui lòng kiểm tra lại.");
            return "register";
        }

        try {
            userService.registerUser(dto);
            return "redirect:/login?success";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
