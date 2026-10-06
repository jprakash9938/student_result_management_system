package com.resultmanager.service;

import com.resultmanager.dto.SubjectDto;
import com.resultmanager.entity.Subject;
import com.resultmanager.exception.DuplicateResourceException;
import com.resultmanager.exception.ResourceNotFoundException;
import com.resultmanager.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    /**
     * Create a new subject in the curriculum.
     */
    public SubjectDto createSubject(SubjectDto dto) {
        if (subjectRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Subject with code " + dto.getCode() + " already exists.");
        }

        Subject subject = Subject.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .department(dto.getDepartment())
                .semester(dto.getSemester())
                .credits(dto.getCredits())
                .build();

        subject = subjectRepository.save(subject);
        return convertToDto(subject);
    }

    /**
     * Update an existing subject.
     */
    public SubjectDto updateSubject(Long id, SubjectDto dto) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        // Check uniqueness of code if changed
        if (!subject.getCode().equalsIgnoreCase(dto.getCode()) && subjectRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Subject with code " + dto.getCode() + " already exists.");
        }

        subject.setCode(dto.getCode());
        subject.setName(dto.getName());
        subject.setDepartment(dto.getDepartment());
        subject.setSemester(dto.getSemester());
        subject.setCredits(dto.getCredits());

        subject = subjectRepository.save(subject);
        return convertToDto(subject);
    }

    /**
     * Fetch all subjects.
     */
    @Transactional(readOnly = true)
    public List<SubjectDto> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Fetch subject by ID.
     */
    @Transactional(readOnly = true)
    public SubjectDto getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
        return convertToDto(subject);
    }

    /**
     * Fetch subjects filtering by department and/or semester.
     */
    @Transactional(readOnly = true)
    public List<SubjectDto> getSubjectsByFilter(String department, Integer semester) {
        List<Subject> subjects;
        if (department != null && !department.isBlank() && semester != null) {
            subjects = subjectRepository.findByDepartmentAndSemester(department, semester);
        } else if (department != null && !department.isBlank()) {
            subjects = subjectRepository.findByDepartment(department);
        } else if (semester != null) {
            subjects = subjectRepository.findBySemester(semester);
        } else {
            subjects = subjectRepository.findAll();
        }

        return subjects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete a subject.
     */
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
        subjectRepository.delete(subject);
    }

    /**
     * Helper mapping entity to DTO.
     */
    public SubjectDto convertToDto(Subject subject) {
        return SubjectDto.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .department(subject.getDepartment())
                .semester(subject.getSemester())
                .credits(subject.getCredits())
                .build();
    }
}
