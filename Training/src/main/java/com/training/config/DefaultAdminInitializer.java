package com.training.config;

import com.training.entity.Admin;
import com.training.entity.User;
import com.training.enums.Role;
import com.training.repo.AdminRepository;
import com.training.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.admin.email:admin@codextechnology.com}")
    private String adminEmail;

    @org.springframework.beans.factory.annotation.Value("${app.admin.password:ChangeMe@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            User adminUser = User.builder()
                    .fullName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .status("ACTIVE")
                    .build();

            adminUser = userRepository.save(adminUser);

            Admin admin = Admin.builder()
                    .user(adminUser)
                    .build();

            adminRepository.save(admin);
            log.info("Default Admin created successfully with email: {}", adminEmail);
        } else {
            log.info("Default Admin already exists.");
        }
    }
}
