package com.example.movie.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Tự động thêm thông tin người dùng đăng nhập vào Model của MỌI trang.
 * Nhờ vậy các template chỉ cần dùng ${isLoggedIn} và ${loginName} (th:if thường),
 * không phụ thuộc vào dialect thymeleaf-extras-springsecurity (chưa tương thích Spring Security 7).
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute
    public void addUserAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        model.addAttribute("isLoggedIn", loggedIn);
        model.addAttribute("isAdmin", loggedIn && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        if (loggedIn) {
            model.addAttribute("loginName", auth.getName());
        }
    }
}
