package com.ahmed.hospital.config;

import com.ahmed.hospital.user.entity.Role;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Default admin password sourced from environment variable via profile-specific
     * application properties.
     *
     * dev profile  → supplies "ChangeMe@Local123" as default (application-dev.properties)
     * prod profile → ADMIN_DEFAULT_PASSWORD env var is REQUIRED; no fallback (fails fast)
     *
     * The @Value here intentionally has no Java-level default so that the property
     * files remain the single source of truth for profile behaviour.
     */
    @Value("${admin.default-password}")
    private String adminDefaultPassword;

    @Override
    public void run(String... args) {

        String adminEmail = "admin@hospital.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(
                            passwordEncoder.encode(adminDefaultPassword)
                    )
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();

            userRepository.save(admin);

            log.info(
                    "Admin account seeded. Set ADMIN_DEFAULT_PASSWORD env var before first run in production."
            );
        }

        String receptionistEmail = "receptionist@hospital.com";
        if (!userRepository.existsByEmail(receptionistEmail)) {
            User receptionist = User.builder()
                    .firstName("Hospital")
                    .lastName("Receptionist")
                    .email(receptionistEmail)
                    .password(
                            passwordEncoder.encode(adminDefaultPassword)
                    )
                    .role(Role.RECEPTIONIST)
                    .enabled(true)
                    .build();

            userRepository.save(receptionist);

            log.info("Receptionist account seeded.");
        }
    }
}