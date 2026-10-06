package com.resultmanager.service;

import com.resultmanager.dto.StudentDto;
import com.resultmanager.entity.Role;
import com.resultmanager.entity.Student;
import com.resultmanager.entity.User;
import com.resultmanager.exception.DuplicateResourceException;
import com.resultmanager.exception.ResourceNotFoundException;
import com.resultmanager.repository.StudentRepository;
import com.resultmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new student, automatically creating their associated User login profile.
     */
    public StudentDto createStudent(StudentDto dto) {
        if (studentRepository.existsByRollNumber(dto.getRollNumber())) {
            throw new DuplicateResourceException("Student with Roll Number " + dto.getRollNumber() + " already exists.");
        }
        if (userRepository.existsByUsername(dto.getRollNumber())) {
            throw new DuplicateResourceException("Username " + dto.getRollNumber() + " is already taken.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email " + dto.getEmail() + " is already registered.");
        }

        // Create User Entity
        User user = User.builder()
                .username(dto.getRollNumber())
                .password(passwordEncoder.encode(dto.getPassword())) // Admin provides the password
                .role(Role.ROLE_STUDENT)
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .build();
        user = userRepository.save(user);

        // Create Student Entity
        Student student = Student.builder()
                .rollNumber(dto.getRollNumber())
                .department(dto.getDepartment())
                .currentSemester(dto.getCurrentSemester())
                .user(user)
                .build();
        student = studentRepository.save(student);

        return convertToDto(student);
    }

    /**
     * Update existing student data, including their associated User login profile.
     */
    public StudentDto updateStudent(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));

        // Check roll number uniqueness if changed
        if (!student.getRollNumber().equalsIgnoreCase(dto.getRollNumber())) {
            if (studentRepository.existsByRollNumber(dto.getRollNumber())) {
                throw new DuplicateResourceException("Student with Roll Number " + dto.getRollNumber() + " already exists.");
            }
            if (userRepository.existsByUsername(dto.getRollNumber())) {
                throw new DuplicateResourceException("Username " + dto.getRollNumber() + " is already taken.");
            }
        }

        // Check email uniqueness if changed
        User user = student.getUser();
        if (!user.getEmail().equalsIgnoreCase(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email " + dto.getEmail() + " is already registered.");
        }

        // Update Student
        student.setRollNumber(dto.getRollNumber());
        student.setDepartment(dto.getDepartment());
        student.setCurrentSemester(dto.getCurrentSemester());

        // Update Associated User details
        user.setUsername(dto.getRollNumber());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        userRepository.save(user);

        student = studentRepository.save(student);
        return convertToDto(student);
    }

    /**
     * Retrieve student by ID.
     */
    public StudentDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        return convertToDto(student);
    }

    /**
     * Retrieve student by Roll Number.
     */
    public StudentDto getStudentByRollNumber(String rollNumber) {
        Student student = studentRepository.findByRollNumber(rollNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with Roll Number: " + rollNumber));
        return convertToDto(student);
    }

    /**
     * Search and filter students.
     */
    @Transactional(readOnly = true)
    public List<StudentDto> searchStudents(String search, String department, Integer semester) {
        List<Student> students = studentRepository.searchStudents(search, department, semester);
        return students.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete a student by ID (this also deletes their associated User due to cascade rules).
     */
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        studentRepository.delete(student);
    }

    /**
     * Helper mapping entity to DTO.
     */
    public StudentDto convertToDto(Student student) {
        return StudentDto.builder()
                .id(student.getId())
                .rollNumber(student.getRollNumber())
                .fullName(student.getUser().getFullName())
                .email(student.getUser().getEmail())
                .department(student.getDepartment())
                .currentSemester(student.getCurrentSemester())
                .build();
    }
}
