package com.example.movie.service;

import com.example.movie.dto.ChangePasswordDto;
import com.example.movie.dto.RegisterDto;
import com.example.movie.model.Role;
import com.example.movie.model.User;
import com.example.movie.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validator validator;

    public void registerUser(RegisterDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ.");
        }

        Set<ConstraintViolation<RegisterDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }

        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword())); // Băm mật khẩu bằng BCrypt
        String phone = dto.getPhone() == null ? null : dto.getPhone().trim();
        user.setPhone(phone == null || phone.isEmpty() ? null : phone);
        user.setRole(Role.CUSTOMER); // Mặc định là Khách hàng

        userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản đang đăng nhập."));
    }

    public void changePassword(String email, ChangePasswordDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Thông tin đổi mật khẩu không hợp lệ.");
        }

        Set<ConstraintViolation<ChangePasswordDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }

        User user = findByEmail(email);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp với mật khẩu mới.");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }
}
