package com.resultmanager.controller;

import com.resultmanager.entity.Role;
import com.resultmanager.entity.Student;
import com.resultmanager.entity.User;
import com.resultmanager.repository.StudentRepository;
import com.resultmanager.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final StudentRepository studentRepository;

    public AuthController(AuthService authService, StudentRepository studentRepository) {
        this.authService = authService;
        this.studentRepository = studentRepository;
    }

    /**
     * Retrieve details of the currently logged-in user.
     * Used by the client-side JS to verify session status, role, and retrieve student IDs.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Optional<User> currentUserOpt = authService.getCurrentUser();
        
        if (currentUserOpt.isEmpty()) {
            Map<String, Object> anonymous = new HashMap<>();
            anonymous.put("authenticated", false);
            return ResponseEntity.ok(anonymous);
        }

        User user = currentUserOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("username", user.getUsername());
        response.put("role", user.getRole().name());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());

        // If the user is a student, return their associated student entity's primary key ID
        if (user.getRole() == Role.ROLE_STUDENT) {
            Optional<Student> studentOpt = studentRepository.findByRollNumber(user.getUsername());
            studentOpt.ifPresent(student -> response.put("studentId", student.getId()));
        }

        return ResponseEntity.ok(response);
    }
}
