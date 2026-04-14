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

import java.util.List;
import java.util.Set;

@Configuration
public class DataSeeder {

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
                                      RoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {

            // =====================================================
            //  BƯỚC 1: SEED TẤT CẢ ROLES
            // =====================================================
            List<RoleDef> roleDefs = List.of(
                new RoleDef("ROLE_ADMIN",      "Quản trị viên",
                    "Toàn quyền hệ thống"),
                new RoleDef("ROLE_MANAGER",    "Quản lý",
                    "Quản lý menu, voucher, khuyến mãi, đơn hàng"),
                new RoleDef("ROLE_ACCOUNTANT", "Kế toán",
                    "Xem báo cáo doanh thu, xuất file kế toán"),
                new RoleDef("ROLE_HR",         "Nhân sự",
                    "Quản lý tài khoản nhân viên"),
                new RoleDef("ROLE_STAFF",      "Nhân viên",
                    "Xem & cập nhật trạng thái đơn hàng"),
                new RoleDef("ROLE_CUSTOMER",   "Khách hàng",
                    "Tài khoản khách hàng mua hàng")
            );

            for (RoleDef def : roleDefs) {
                if (roleRepository.findByCode(def.code()).isEmpty()) {
                    roleRepository.save(Role.builder()
                            .code(def.code())
                            .name(def.name())
                            .description(def.description())
                            .build());
                    System.out.println("✓ Đã tạo role: " + def.code());
                }
            }

            // =====================================================
            //  BƯỚC 2: SEED TÀI KHOẢN SUPER ADMIN
            // =====================================================
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
                System.out.println("✓ Đã tạo tài khoản Admin: admin@gmail.com");
            }

            // =====================================================
            //  BƯỚC 3: SEED TÀI KHOẢN MẪU CHO TỪNG ROLE (Dev/Test)
            //  Xóa khối này khi deploy production
            // =====================================================
            seedDevAccount(userRepository, roleRepository, passwordEncoder,
                    "manager@gmail.com",    "ROLE_MANAGER",    "Nguyễn Quản Lý");
            seedDevAccount(userRepository, roleRepository, passwordEncoder,
                    "accountant@gmail.com", "ROLE_ACCOUNTANT", "Trần Kế Toán");
            seedDevAccount(userRepository, roleRepository, passwordEncoder,
                    "hr@gmail.com",         "ROLE_HR",         "Lê Nhân Sự");
            seedDevAccount(userRepository, roleRepository, passwordEncoder,
                    "staff@gmail.com",      "ROLE_STAFF",      "Phạm Nhân Viên");
        };
    }

    private void seedDevAccount(UserRepository userRepo, RoleRepository roleRepo,
                                 PasswordEncoder encoder,
                                 String email, String roleCode, String fullName) {
        if (userRepo.findByEmail(email).isEmpty()) {
            roleRepo.findByCode(roleCode).ifPresent(role -> {
                userRepo.save(User.builder()
                        .email(email)
                        .passwordHash(encoder.encode("123456"))
                        .fullName(fullName)
                        .isVerified(true)
                        .isActive(true)
                        .roles(Set.of(role))
                        .build());
                System.out.println("✓ Dev account: " + email + " / 123456 [" + roleCode + "]");
            });
        }
    }

    /** Record nội bộ để định nghĩa role */
    private record RoleDef(String code, String name, String description) {}
}
