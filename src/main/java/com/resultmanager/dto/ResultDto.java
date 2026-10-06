package com.resultmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultDto {
    private Long id;
    private Long studentId;
    private String rollNumber;
    private String studentName;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer semester;
    private Double marks;
    private String grade;
    private String status; // DRAFT or PUBLISHED
}
