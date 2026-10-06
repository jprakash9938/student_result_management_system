package com.resultmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelUploadResponse {
    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private List<String> errors;
    private List<ResultDto> importedResults;
}
