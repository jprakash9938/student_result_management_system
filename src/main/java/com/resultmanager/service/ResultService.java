package com.resultmanager.service;

import com.resultmanager.dto.DashboardStatsDto;
import com.resultmanager.dto.ResultDto;
import com.resultmanager.dto.ResultEntryDto;
import com.resultmanager.dto.StudentDashboardDto;
import com.resultmanager.entity.*;
import com.resultmanager.exception.DuplicateResourceException;
import com.resultmanager.exception.InvalidDataException;
import com.resultmanager.exception.ResourceNotFoundException;
import com.resultmanager.repository.ResultRepository;
import com.resultmanager.repository.StudentRepository;
import com.resultmanager.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResultService {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public ResultService(ResultRepository resultRepository,
                         StudentRepository studentRepository,
                         SubjectRepository subjectRepository) {
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    /**
     * Enter a new result for a student in DRAFT state.
     */
    public ResultDto enterMark(ResultEntryDto dto) {
        Student student = studentRepository.findByRollNumber(dto.getRollNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with Roll Number: " + dto.getRollNumber()));

        Subject subject = subjectRepository.findByCode(dto.getSubjectCode())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with code: " + dto.getSubjectCode()));

        // Check if marks already exist for this student, subject and semester
        Optional<Result> existingResult = resultRepository.findByStudentIdAndSubjectIdAndSemester(
                student.getId(), subject.getId(), dto.getSemester());

        if (existingResult.isPresent()) {
            throw new DuplicateResourceException("Marks already entered for Student " + dto.getRollNumber() + 
                    " in Subject " + dto.getSubjectCode() + " for semester " + dto.getSemester() + ". Please update instead.");
        }

        String grade = calculateGrade(dto.getMarks());

        Result result = Result.builder()
                .student(student)
                .subject(subject)
                .semester(dto.getSemester())
                .marks(dto.getMarks())
                .grade(grade)
                .status(ResultStatus.DRAFT)
                .build();

        result = resultRepository.save(result);
        return convertToDto(result);
    }

    /**
     * Update marks for an existing result.
     */
    public ResultDto updateMark(Long id, Double marks) {
        if (marks < 0 || marks > 100) {
            throw new InvalidDataException("Marks must be between 0 and 100.");
        }

        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result record not found with ID: " + id));

        result.setMarks(marks);
        result.setGrade(calculateGrade(marks));
        result = resultRepository.save(result);

        return convertToDto(result);
    }

    /**
     * Delete a result record.
     */
    public void deleteResult(Long id) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result record not found with ID: " + id));
        resultRepository.delete(result);
    }

    /**
     * Publish draft results.
     */
    public void publishResults(List<Long> resultIds) {
        for (Long id : resultIds) {
            Result result = resultRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Result record not found with ID: " + id));

            if (result.getStatus() == ResultStatus.DRAFT) {
                result.setStatus(ResultStatus.PUBLISHED);
                resultRepository.save(result);
            }
        }
    }

    /**
     * Retrieve results for a student. Filter by semester if specified.
     */
    @Transactional(readOnly = true)
    public List<ResultDto> getStudentResults(Long studentId, Integer semester, boolean includeDrafts) {
        List<Result> results;
        if (semester != null) {
            if (includeDrafts) {
                results = resultRepository.findByStudentIdAndSemester(studentId, semester);
            } else {
                results = resultRepository.findByStudentIdAndSemesterAndStatus(studentId, semester, ResultStatus.PUBLISHED);
            }
        } else {
            if (includeDrafts) {
                results = resultRepository.findByStudentId(studentId);
            } else {
                results = resultRepository.findByStudentIdAndStatus(studentId, ResultStatus.PUBLISHED);
            }
        }

        return results.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Fetch list of all results (both draft & published) for teacher views.
     */
    @Transactional(readOnly = true)
    public List<ResultDto> getAllResults() {
        return resultRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get Student Dashboard DTO containing profile, semester grades, and CGPA.
     */
    @Transactional(readOnly = true)
    public StudentDashboardDto getStudentDashboard(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        // Fetch only published results for student dashboard
        List<Result> publishedResults = resultRepository.findByStudentIdAndStatus(studentId, ResultStatus.PUBLISHED);
        List<ResultDto> resultDtos = publishedResults.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // Group by semester and calculate SGPA
        Map<Integer, List<Result>> resultsBySemester = publishedResults.stream()
                .collect(Collectors.groupingBy(Result::getSemester));

        Map<Integer, Double> semesterSgpa = new TreeMap<>();
        for (Map.Entry<Integer, List<Result>> entry : resultsBySemester.entrySet()) {
            semesterSgpa.put(entry.getKey(), calculateSgpaFromResults(entry.getValue()));
        }

        double cgpa = calculateCgpaFromResults(publishedResults);

        return StudentDashboardDto.builder()
                .studentId(student.getId())
                .fullName(student.getUser().getFullName())
                .rollNumber(student.getRollNumber())
                .email(student.getUser().getEmail())
                .department(student.getDepartment())
                .currentSemester(student.getCurrentSemester())
                .semesterSgpa(semesterSgpa)
                .cgpa(cgpa)
                .results(resultDtos)
                .build();
    }

    /**
     * Get total overview counts for the teacher's dashboard.
     */
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        return DashboardStatsDto.builder()
                .totalStudents(studentRepository.count())
                .totalSubjects(subjectRepository.count())
                .publishedResults(resultRepository.countByStatus(ResultStatus.PUBLISHED))
                .pendingResults(resultRepository.countByStatus(ResultStatus.DRAFT))
                .build();
    }

    // --- Core Result Calculation Formulas ---

    /**
     * Configurable grading logic mapping score ranges to letter grades.
     */
    public String calculateGrade(Double marks) {
        if (marks == null) return "F";
        if (marks >= 90) return "A+";
        if (marks >= 80) return "A";
        if (marks >= 70) return "B";
        if (marks >= 60) return "C";
        if (marks >= 50) return "D";
        return "F";
    }

    /**
     * Map letter grades to Grade Points.
     */
    private double getGradePoint(String grade) {
        return switch (grade) {
            case "A+" -> 10.0;
            case "A" -> 9.0;
            case "B" -> 8.0;
            case "C" -> 7.0;
            case "D" -> 6.0;
            default -> 0.0;
        };
    }

    /**
     * Calculate Semester GPA (SGPA) = Sum(GP * Credits) / Sum(Credits)
     */
    private double calculateSgpaFromResults(List<Result> semesterResults) {
        if (semesterResults == null || semesterResults.isEmpty()) {
            return 0.0;
        }

        double totalPoints = 0.0;
        int totalCredits = 0;

        for (Result r : semesterResults) {
            double gp = getGradePoint(r.getGrade());
            int credits = r.getSubject().getCredits();
            totalPoints += (gp * credits);
            totalCredits += credits;
        }

        if (totalCredits == 0) return 0.0;

        double sgpa = totalPoints / totalCredits;
        return round(sgpa, 2);
    }

    /**
     * Calculate Cumulative GPA (CGPA) = Sum(GP * Credits) / Sum(Credits) across all published semesters
     */
    private double calculateCgpaFromResults(List<Result> allPublishedResults) {
        if (allPublishedResults == null || allPublishedResults.isEmpty()) {
            return 0.0;
        }

        double totalPoints = 0.0;
        int totalCredits = 0;

        for (Result r : allPublishedResults) {
            double gp = getGradePoint(r.getGrade());
            int credits = r.getSubject().getCredits();
            totalPoints += (gp * credits);
            totalCredits += credits;
        }

        if (totalCredits == 0) return 0.0;

        double cgpa = totalPoints / totalCredits;
        return round(cgpa, 2);
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Map Result entity to ResultDto.
     */
    public ResultDto convertToDto(Result result) {
        return ResultDto.builder()
                .id(result.getId())
                .studentId(result.getStudent().getId())
                .rollNumber(result.getStudent().getRollNumber())
                .studentName(result.getStudent().getUser().getFullName())
                .subjectId(result.getSubject().getId())
                .subjectCode(result.getSubject().getCode())
                .subjectName(result.getSubject().getName())
                .semester(result.getSemester())
                .marks(result.getMarks())
                .grade(result.getGrade())
                .status(result.getStatus().name())
                .build();
    }
}
