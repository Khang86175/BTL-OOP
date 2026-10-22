package com.example.movie.config;

import com.example.movie.model.*;
import com.example.movie.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Tài khoản kiểm thử chỉ tồn tại trong profile demo với DB H2 trong bộ nhớ. */
@Component
@Profile("demo")
public class DemoAdminData implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    public DemoAdminData(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByEmail("admin@cineticket.test")) return;
        User admin = new User();
        admin.setFullName("Admin Demo");
        admin.setEmail("admin@cineticket.test");
        admin.setPasswordHash(passwords.encode("DemoAdmin123!"));
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
