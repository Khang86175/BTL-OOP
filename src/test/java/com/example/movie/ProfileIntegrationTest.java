package com.example.movie;

import com.example.movie.model.Role;
import com.example.movie.model.User;
import com.example.movie.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    @Test
    void profilePageRequiresAuthentication() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void signedInUserCanSeeOwnProfileDetails() throws Exception {
        User account = new User();
        account.setFullName("Nguyễn Văn A");
        account.setEmail("profile@example.com");
        account.setPasswordHash("not-used-in-this-test");
        account.setPhone("0987654321");
        account.setRole(Role.CUSTOMER);
        account.setCreatedAt(LocalDateTime.of(2025, 1, 2, 13, 45));
        userRepository.save(account);

        mvc.perform(get("/profile").with(user("profile@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Văn A")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("profile@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0987654321")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khách hàng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("02/01/2025 13:45")));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "profile@example.com", roles = "CUSTOMER")
    void navbarReplacesLoginEmailWithProfileIcon() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"Hồ sơ người dùng\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/profile")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng xuất")));
    }
}
