package com.resultmanager.controller;

import com.resultmanager.dto.*;
import com.resultmanager.entity.Role;
import com.resultmanager.entity.Student;
import com.resultmanager.entity.User;
import com.resultmanager.exception.InvalidDataException;
import com.resultmanager.repository.StudentRepository;
import com.resultmanager.service.AuthService;
import com.resultmanager.service.ExcelUploadService;
import com.resultmanager.service.PdfGenerationService;
import com.resultmanager.service.ResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;
    private final ExcelUploadService excelUploadService;
    private final PdfGenerationService pdfGenerationService;
    private final AuthService authService;
    private final StudentRepository studentRepository;

    public ResultController(ResultService resultService,
                            ExcelUploadService excelUploadService,
                            PdfGenerationService pdfGenerationService,
                            AuthService authService,
                            StudentRepository studentRepository) {
        this.resultService = resultService;
        this.excelUploadService = excelUploadService;
        this.pdfGenerationService = pdfGenerationService;
        this.authService = authService;
        this.studentRepository = studentRepository;
    }

    /**
     * Retrieve list of all results (both draft/published) for teacher management tables.
     */
    @GetMapping
    public ResponseEntity<List<ResultDto>> getAllResults() {
        List<ResultDto> results = resultService.getAllResults();
        return ResponseEntity.ok(results);
    }

    /**
     * Retrieve statistics overview counts (total student, total subject, published, drafts).
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        DashboardStatsDto stats = resultService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Manually enter marks for a student.
     */
    @PostMapping
    public ResponseEntity<ResultDto> enterMark(@Valid @RequestBody ResultEntryDto dto) {
        ResultDto created = resultService.enterMark(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update marks for a result record.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResultDto> updateMark(@PathVariable Long id, @RequestBody Map<String, Double> payload) {
        if (payload == null || !payload.containsKey("marks")) {
            throw new InvalidDataException("Payload must contain key 'marks'.");
        }
        Double marks = payload.get("marks");
        ResultDto updated = resultService.updateMark(id, marks);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a result record.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResult(@PathVariable Long id) {
        resultService.deleteResult(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publish draft results to make them visible to students.
     */
    @PostMapping("/publish")
    public ResponseEntity<Void> publishResults(@RequestBody List<Long> resultIds) {
        resultService.publishResults(resultIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Import marks from an Excel spreadsheet.
     */
    @PostMapping("/upload")
    public ResponseEntity<ExcelUploadResponse> uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidDataException("Uploaded file is empty.");
        }
        ExcelUploadResponse response = excelUploadService.uploadExcel(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve results for a specific student.
     * Enforces IDOR security checks: Students can only retrieve their own records.
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ResultDto>> getStudentResults(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer semester) {
        
        verifyStudentResourceOwnership(studentId);

        // If authenticated user is a student, only retrieve published results (drafts excluded)
        boolean includeDrafts = true;
        Optional<User> currentUserOpt = authService.getCurrentUser();
        if (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == Role.ROLE_STUDENT) {
            includeDrafts = false;
        }

        List<ResultDto> results = resultService.getStudentResults(studentId, semester, includeDrafts);
        return ResponseEntity.ok(results);
    }

    /**
     * Retrieve dashboard info for a student (includes profile data, semester SGPAs, overall CGPA, and results).
     * Enforces IDOR security checks: Students can only query their own dashboard.
     */
    @GetMapping("/student/{studentId}/dashboard")
    public ResponseEntity<StudentDashboardDto> getStudentDashboard(@PathVariable Long studentId) {
        verifyStudentResourceOwnership(studentId);
        StudentDashboardDto dashboard = resultService.getStudentDashboard(studentId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Download semester transcript as a PDF.
     * Enforces IDOR security checks: Students can only download their own transcript.
     */
    @GetMapping("/student/{studentId}/pdf")
    public ResponseEntity<byte[]> downloadResultPdf(
            @PathVariable Long studentId,
            @RequestParam Integer semester) {
        
        verifyStudentResourceOwnership(studentId);

        byte[] pdfBytes = pdfGenerationService.generateResultPdf(studentId, semester);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=Semester_" + semester + "_Transcript.pdf")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * Helper to verify if the currently logged-in user is authorized to access student resources.
     * Prevents IDOR (Insecure Direct Object Reference).
     */
    private void verifyStudentResourceOwnership(Long studentId) {
        Optional<User> currentUserOpt = authService.getCurrentUser();
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            if (user.getRole() == Role.ROLE_STUDENT) {
                Student student = studentRepository.findByRollNumber(user.getUsername())
                        .orElseThrow(() -> new AccessDeniedException("No student mapping found for this session user."));
                
                if (!student.getId().equals(studentId)) {
                    throw new AccessDeniedException("You are not authorized to access resources of another student.");
                }
            }
        } else {
            throw new AccessDeniedException("Full authentication is required to access this resource.");
        }
    }
}
