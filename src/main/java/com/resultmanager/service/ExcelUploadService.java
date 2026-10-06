package com.resultmanager.service;

import com.resultmanager.dto.ExcelUploadResponse;
import com.resultmanager.dto.ResultDto;
import com.resultmanager.entity.*;
import com.resultmanager.repository.ResultRepository;
import com.resultmanager.repository.StudentRepository;
import com.resultmanager.repository.SubjectRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExcelUploadService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ResultRepository resultRepository;
    private final ResultService resultService;

    public ExcelUploadService(StudentRepository studentRepository,
                              SubjectRepository subjectRepository,
                              ResultRepository resultRepository,
                              ResultService resultService) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.resultRepository = resultRepository;
        this.resultService = resultService;
    }

    /**
     * Parse Excel spreadsheet (.xlsx) and import marks into the database.
     * Invalid rows are skipped and errors are collected to report to the teacher.
     */
    public ExcelUploadResponse uploadExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<ResultDto> importedResults = new ArrayList<>();
        int totalRows = 0;
        int successfulRows = 0;
        int failedRows = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();

            // Iterate through rows (skip header row 0)
            for (int i = 1; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                totalRows++;
                int displayRowNumber = i + 1; // 1-indexed for user display

                try {
                    String rollNumber = getCellValueAsString(row.getCell(0));
                    String subjectCode = getCellValueAsString(row.getCell(1));
                    String semesterStr = getCellValueAsString(row.getCell(2));
                    String marksStr = getCellValueAsString(row.getCell(3));

                    // Validation 1: Blank checks
                    if (rollNumber.isBlank()) {
                        throw new IllegalArgumentException("Roll Number is empty.");
                    }
                    if (subjectCode.isBlank()) {
                        throw new IllegalArgumentException("Subject Code is empty.");
                    }
                    if (semesterStr.isBlank()) {
                        throw new IllegalArgumentException("Semester is empty.");
                    }
                    if (marksStr.isBlank()) {
                        throw new IllegalArgumentException("Marks are empty.");
                    }

                    // Validation 2: Number formats
                    int semester;
                    try {
                        // Handle potential floating points in numbers e.g. "3.0"
                        semester = (int) Double.parseDouble(semesterStr);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid semester number format: " + semesterStr);
                    }

                    double marks;
                    try {
                        marks = Double.parseDouble(marksStr);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid marks format: " + marksStr);
                    }

                    // Validation 3: Boundary checks
                    if (semester < 1 || semester > 8) {
                        throw new IllegalArgumentException("Semester must be between 1 and 8.");
                    }
                    if (marks < 0 || marks > 100) {
                        throw new IllegalArgumentException("Marks must be between 0 and 100.");
                    }

                    // Validation 4: Database entity presence
                    Student student = studentRepository.findByRollNumber(rollNumber)
                            .orElseThrow(() -> new IllegalArgumentException("Student not found with Roll Number: " + rollNumber));

                    Subject subject = subjectRepository.findByCode(subjectCode)
                            .orElseThrow(() -> new IllegalArgumentException("Subject not found with code: " + subjectCode));

                    // Verify subject belongs to the student's department or semester (optional warn, but here we require valid subject)
                    
                    // Validation 5: Process Save or Update (DRAFT state)
                    Optional<Result> existingResult = resultRepository.findByStudentIdAndSubjectIdAndSemester(
                            student.getId(), subject.getId(), semester);

                    Result result;
                    String grade = resultService.calculateGrade(marks);

                    if (existingResult.isPresent()) {
                        result = existingResult.get();
                        result.setMarks(marks);
                        result.setGrade(grade);
                        result.setStatus(ResultStatus.DRAFT); // resets to draft for review
                    } else {
                        result = Result.builder()
                                .student(student)
                                .subject(subject)
                                .semester(semester)
                                .marks(marks)
                                .grade(grade)
                                .status(ResultStatus.DRAFT)
                                .build();
                    }

                    result = resultRepository.save(result);
                    importedResults.add(resultService.convertToDto(result));
                    successfulRows++;

                } catch (Exception e) {
                    failedRows++;
                    errors.add("Row " + displayRowNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("Failed to process Excel workbook: " + e.getMessage());
        }

        return ExcelUploadResponse.builder()
                .totalRows(totalRows)
                .successfulRows(successfulRows)
                .failedRows(failedRows)
                .errors(errors)
                .importedResults(importedResults)
                .build();
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numericVal = cell.getNumericCellValue();
                // If it is mathematically an integer, format without decimals
                if (numericVal == (long) numericVal) {
                    return String.valueOf((long) numericVal);
                }
                return String.valueOf(numericVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue.getCellType() == CellType.NUMERIC) {
                        double val = cellValue.getNumberValue();
                        if (val == (long) val) {
                            return String.valueOf((long) val);
                        }
                        return String.valueOf(val);
                    } else if (cellValue.getCellType() == CellType.STRING) {
                        return cellValue.getStringValue().trim();
                    }
                    return cell.getCellFormula();
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            case BLANK:
            default:
                return "";
        }
    }
}
