package com.example.movie;

import com.example.movie.model.Role;
import com.example.movie.model.User;
import com.example.movie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordChangeIntegrationTest {
    private static final String EMAIL = "password@example.com";
    private static final String CURRENT_PASSWORD = "current-secret";

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void createAccount() {
        User account = new User();
        account.setFullName("Nguyễn Văn A");
        account.setEmail(EMAIL);
        account.setPasswordHash(passwordEncoder.encode(CURRENT_PASSWORD));
        account.setRole(Role.CUSTOMER);
        userRepository.save(account);
    }

    @Test
    void changePasswordPageRequiresLogin() throws Exception {
        mvc.perform(get("/profile/change-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void changesPasswordAfterValidatingCurrentAndConfirmation() throws Exception {
        submit(CURRENT_PASSWORD, "new-secret", "new-secret")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordChanged"));

        String savedHash = userRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches("new-secret", savedHash)).isTrue();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, savedHash)).isFalse();
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutUpdatingHash() throws Exception {
        submit("incorrect", "new-secret", "new-secret")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu hiện tại không chính xác.")));

        assertCurrentPasswordUnchanged();
    }

    @Test
    void rejectsMismatchedConfirmationWithoutUpdatingHash() throws Exception {
        submit(CURRENT_PASSWORD, "new-secret", "different-secret")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu xác nhận không khớp với mật khẩu mới.")));

        assertCurrentPasswordUnchanged();
    }

    @Test
    void rejectsNewPasswordThatMatchesCurrentPassword() throws Exception {
        submit(CURRENT_PASSWORD, CURRENT_PASSWORD, CURRENT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mật khẩu mới không được trùng với mật khẩu hiện tại.")));

        assertCurrentPasswordUnchanged();
    }

    @Test
    void blankPasswordFieldsAreRejected() throws Exception {
        submit("", "", "")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vui lòng nhập mật khẩu hiện tại.")))
                .andExpect(content().string(containsString("Vui lòng nhập mật khẩu mới.")))
                .andExpect(content().string(containsString("Vui lòng xác nhận mật khẩu mới.")));

        assertCurrentPasswordUnchanged();
    }

    private org.springframework.test.web.servlet.ResultActions submit(String current, String next, String confirmation)
            throws Exception {
        MockHttpServletRequestBuilder request = post("/profile/change-password")
                .with(user(EMAIL).roles("CUSTOMER"))
                .with(csrf())
                .param("currentPassword", current)
                .param("newPassword", next)
                .param("confirmPassword", confirmation);
        return mvc.perform(request);
    }

    private void assertCurrentPasswordUnchanged() {
        String savedHash = userRepository.findByEmail(EMAIL).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches(CURRENT_PASSWORD, savedHash)).isTrue();
        assertThat(passwordEncoder.matches("new-secret", savedHash)).isFalse();
    }
}
