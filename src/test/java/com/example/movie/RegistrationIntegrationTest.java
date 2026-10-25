package com.example.movie;

import com.example.movie.dto.RegisterDto;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired UserService userService;

    @Test
    void registrationRejectsLettersInPhoneBeforeSaving() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Nguyễn Văn A")
                        .param("email", "a@example.com")
                        .param("password", "secret123")
                        .param("phone", "09123abc789"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registerDto", "phone"))
                .andExpect(content().string(containsString("Số điện thoại phải gồm 10 hoặc 11 chữ số.")));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void validRegistrationStoresPhoneAndEmptyPhoneAsNull() {
        RegisterDto dto = registration("a@example.com", "0987654321");
        userService.registerUser(dto);

        assertThat(userRepository.findByEmail("a@example.com").orElseThrow().getPhone())
                .isEqualTo("0987654321");

        RegisterDto withoutPhone = registration("b@example.com", "");
        userService.registerUser(withoutPhone);
        assertThat(userRepository.findByEmail("b@example.com").orElseThrow().getPhone()).isNull();
    }

    @Test
    void serviceRejectsInvalidPhoneEvenWhenCalledWithoutWebValidation() {
        RegisterDto dto = registration("a@example.com", "phone123456");

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Số điện thoại phải gồm 10 hoặc 11 chữ số.");
        assertThat(userRepository.count()).isZero();
    }

    private RegisterDto registration(String email, String phone) {
        RegisterDto dto = new RegisterDto();
        dto.setFullName("Nguyễn Văn A");
        dto.setEmail(email);
        dto.setPassword("secret123");
        dto.setPhone(phone);
        return dto;
    }
}
