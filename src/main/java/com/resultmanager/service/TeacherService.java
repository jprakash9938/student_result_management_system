package com.resultmanager.service;

import com.resultmanager.dto.TeacherDto;
import com.resultmanager.entity.Role;
import com.resultmanager.entity.User;
import com.resultmanager.exception.DuplicateResourceException;
import com.resultmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TeacherDto createTeacher(TeacherDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username " + dto.getUsername() + " is already taken.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email " + dto.getEmail() + " is already registered.");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.ROLE_TEACHER)
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .build();
        user = userRepository.save(user);

        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public List<TeacherDto> getAllTeachers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ROLE_TEACHER)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public void deleteTeacher(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.ROLE_TEACHER)
                .orElseThrow(() -> new RuntimeException("Teacher not found with ID: " + id));
        userRepository.delete(user);
    }

    private TeacherDto convertToDto(User user) {
        return TeacherDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                // Don't send back password
                .build();
    }
}
