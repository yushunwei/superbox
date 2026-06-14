package com.superbox.config;

import com.superbox.app.auth.entity.User;
import com.superbox.app.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User existing = userMapper.findByUsername("admin");
        if (existing == null) {
            log.info("Creating default admin user...");
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setDisplayName("管理员");
            admin.setPreferredLanguage("zh-CN");
            userMapper.insert(admin);
            log.info("Default admin user created (username: admin, password: admin123)");
        }
    }
}
