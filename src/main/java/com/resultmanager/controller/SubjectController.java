package com.resultmanager.controller;

import com.resultmanager.dto.SubjectDto;
import com.resultmanager.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    /**
     * Retrieve subjects, with optional filters for department and/or semester.
     */
    @GetMapping
    public ResponseEntity<List<SubjectDto>> getSubjects(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer semester) {
        List<SubjectDto> subjects = subjectService.getSubjectsByFilter(department, semester);
        return ResponseEntity.ok(subjects);
    }

    /**
     * Retrieve a specific subject by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubjectDto> getSubjectById(@PathVariable Long id) {
        SubjectDto subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(subject);
    }

    /**
     * Create a new subject in the curriculum.
     */
    @PostMapping
    public ResponseEntity<SubjectDto> createSubject(@Valid @RequestBody SubjectDto subjectDto) {
        SubjectDto created = subjectService.createSubject(subjectDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing subject.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SubjectDto> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectDto subjectDto) {
        SubjectDto updated = subjectService.updateSubject(id, subjectDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a subject from the system.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}
