package com.resultmanager.config;

import com.resultmanager.entity.Role;
import com.resultmanager.entity.User;
import com.resultmanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.logging.Logger;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed or update admin user credentials
        Optional<User> existingAdmin = userRepository.findByUsername("jyotiprakashnayak");
        if (existingAdmin.isEmpty()) {
            Optional<User> oldAdmin = userRepository.findByUsername("admin");
            User admin = oldAdmin.orElseGet(User::new);

            admin.setUsername("jyotiprakashnayak");
            admin.setPassword(passwordEncoder.encode("Jyotyprakash@1234"));
            admin.setRole(Role.ROLE_ADMIN);
            admin.setFullName("Jyotiprakash Nayak");
            admin.setEmail("jyotiprakashnayak@gmail.com");

            userRepository.save(admin);
            LOGGER.info("Admin account initialized successfully.");
        }
    }
}
