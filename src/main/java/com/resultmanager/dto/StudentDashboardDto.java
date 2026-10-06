package com.resultmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDashboardDto {
    private Long studentId;
    private String fullName;
    private String rollNumber;
    private String email;
    private String department;
    private Integer currentSemester;
    private Map<Integer, Double> semesterSgpa; // map from semester number to calculated SGPA
    private Double cgpa; // overall calculated CGPA from published semesters
    private List<ResultDto> results; // list of all published results for this student
}
