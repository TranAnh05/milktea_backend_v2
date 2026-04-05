package com.example.milktea_backend.configs;

import com.example.milktea_backend.entities.Role;
import com.example.milktea_backend.entities.User;
import com.example.milktea_backend.repositories.RoleRepository;
import com.example.milktea_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataSeeder {
    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Tạo Role nếu chưa có
            if (roleRepository.findByCode("ROLE_ADMIN").isEmpty()) {
                roleRepository.save(Role.builder().code("ROLE_ADMIN").name("Quản trị viên").build());
            }
            if (roleRepository.findByCode("ROLE_CUSTOMER").isEmpty()) {
                roleRepository.save(Role.builder().code("ROLE_CUSTOMER").name("Khách hàng").build());
            }

            // 2. Tạo Admin mặc định nếu chưa có
            if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
                Role adminRole = roleRepository.findByCode("ROLE_ADMIN").get();
                User admin = User.builder()
                        .email("admin@gmail.com")
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .fullName("Super Admin")
                        .isVerified(true)
                        .isActive(true)
                        .roles(Set.of(adminRole))
                        .build();
                userRepository.save(admin);
                System.out.println("Đã khởi tạo tài khoản Admin: admin@gmail.com / 123456");
            }
        };
    }
}
