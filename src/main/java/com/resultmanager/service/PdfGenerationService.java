package com.resultmanager.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.resultmanager.entity.Result;
import com.resultmanager.entity.ResultStatus;
import com.resultmanager.entity.Student;
import com.resultmanager.exception.ResourceNotFoundException;
import com.resultmanager.repository.ResultRepository;
import com.resultmanager.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PdfGenerationService {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final ResultService resultService;

    public PdfGenerationService(ResultRepository resultRepository,
                                  StudentRepository studentRepository,
                                  ResultService resultService) {
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.resultService = resultService;
    }

    /**
     * Generate a professional PDF report containing the official mark sheet for a student's semester.
     */
    public byte[] generateResultPdf(Long studentId, Integer semester) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        // Fetch published results for this specific semester
        List<Result> results = resultRepository.findByStudentIdAndSemesterAndStatus(studentId, semester, ResultStatus.PUBLISHED);
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("No published results found for Student " + student.getRollNumber() + " in Semester " + semester);
        }

        // Fetch all published results to calculate CGPA
        List<Result> allPublishedResults = resultRepository.findByStudentIdAndStatus(studentId, ResultStatus.PUBLISHED);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Color Palette
            Color navyPrimary = new Color(28, 54, 115);
            Color textDark = new Color(51, 51, 51);
            Color lightGrayBg = new Color(245, 245, 245);
            Color borderGray = new Color(211, 211, 211);

            // Font definitions
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, navyPrimary);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, navyPrimary);
            Font metaLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textDark);
            Font metaValueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textDark);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, textDark);
            Font tableBodyBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textDark);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

            // 1. College Header
            Paragraph collegeName = new Paragraph("EXEMPLAR INSTITUTE OF TECHNOLOGY", titleFont);
            collegeName.setAlignment(Element.ALIGN_CENTER);
            collegeName.setSpacingAfter(2);
            document.add(collegeName);

            Paragraph examOffice = new Paragraph("OFFICE OF THE CONTROLLER OF EXAMINATIONS\nACADEMIC RECORD TRANSCRIPT", subtitleFont);
            examOffice.setAlignment(Element.ALIGN_CENTER);
            examOffice.setSpacingAfter(10);
            document.add(examOffice);

            // Divider Line
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderWidth(1.5f);
            lineCell.setBorderColor(navyPrimary);
            lineCell.setPadding(0);
            line.addCell(lineCell);
            line.setSpacingAfter(15);
            document.add(line);

            // 2. Student Metadata Table
            PdfPTable metaTable = new PdfPTable(4);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{1.2f, 1.8f, 1.2f, 1.8f});
            metaTable.setSpacingAfter(15);

            addMetaCell(metaTable, "Student Name:", student.getUser().getFullName(), metaLabelFont, metaValueFont);
            addMetaCell(metaTable, "Roll Number:", student.getRollNumber(), metaLabelFont, metaValueFont);
            addMetaCell(metaTable, "Department:", student.getDepartment(), metaLabelFont, metaValueFont);
            addMetaCell(metaTable, "Semester:", String.valueOf(semester), metaLabelFont, metaValueFont);

            document.add(metaTable);

            // Section Header
            Paragraph sectionTitle = new Paragraph("STATEMENT OF MARKS AND GRADES", sectionTitleFont);
            sectionTitle.setSpacingAfter(8);
            document.add(sectionTitle);

            // 3. Grades/Marks Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3.5f, 1.0f, 1.2f, 1.0f});
            table.setSpacingAfter(15);

            // Headers
            addTableHeader(table, "Subject Code", navyPrimary, tableHeaderFont, Element.ALIGN_LEFT);
            addTableHeader(table, "Subject Name", navyPrimary, tableHeaderFont, Element.ALIGN_LEFT);
            addTableHeader(table, "Credits", navyPrimary, tableHeaderFont, Element.ALIGN_CENTER);
            addTableHeader(table, "Marks Obtained", navyPrimary, tableHeaderFont, Element.ALIGN_CENTER);
            addTableHeader(table, "Letter Grade", navyPrimary, tableHeaderFont, Element.ALIGN_CENTER);

            double totalMarks = 0;
            int totalCredits = 0;
            int maxPossibleMarks = results.size() * 100;
            boolean failedAny = false;

            for (Result r : results) {
                totalMarks += r.getMarks();
                totalCredits += r.getSubject().getCredits();
                if ("F".equals(r.getGrade())) {
                    failedAny = true;
                }

                // Add row cells
                addTableCell(table, r.getSubject().getCode(), tableBodyFont, Element.ALIGN_LEFT, borderGray, lightGrayBg);
                addTableCell(table, r.getSubject().getName(), tableBodyFont, Element.ALIGN_LEFT, borderGray, lightGrayBg);
                addTableCell(table, String.valueOf(r.getSubject().getCredits()), tableBodyFont, Element.ALIGN_CENTER, borderGray, lightGrayBg);
                addTableCell(table, String.valueOf(r.getMarks()), tableBodyFont, Element.ALIGN_CENTER, borderGray, lightGrayBg);
                
                // Color Code Failed Grades in Red
                Font gradeFont = "F".equals(r.getGrade()) ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.RED) : tableBodyBoldFont;
                addTableCell(table, r.getGrade(), gradeFont, Element.ALIGN_CENTER, borderGray, lightGrayBg);
            }

            document.add(table);

            // 4. Calculations Summary Grid
            double percentage = (totalMarks / maxPossibleMarks) * 100;
            
            // Calculate SGPA and CGPA using the central formulas
            // Retrieve SGPA and CGPA via the service to ensure consistent rounding
            var dashboard = resultService.getStudentDashboard(studentId);
            double sgpa = dashboard.getSemesterSgpa().getOrDefault(semester, 0.0);
            double cgpa = dashboard.getCgpa() != null ? dashboard.getCgpa() : 0.0;
            
            String passStatus = failedAny ? "FAILED" : "PASSED";
            Font statusFont = failedAny ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED) : FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(34, 139, 34));

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{1.5f, 1.5f, 1.5f, 1.5f});
            summaryTable.setSpacingAfter(35);

            DecimalFormat df = new DecimalFormat("#.##");

            addSummaryCell(summaryTable, "Total Marks:", String.format("%s / %d", df.format(totalMarks), maxPossibleMarks), metaLabelFont, metaValueFont, borderGray);
            addSummaryCell(summaryTable, "Percentage:", df.format(percentage) + "%", metaLabelFont, metaValueFont, borderGray);
            addSummaryCell(summaryTable, "Semester GPA (SGPA):", df.format(sgpa), metaLabelFont, metaValueFont, borderGray);
            addSummaryCell(summaryTable, "Cumulative GPA (CGPA):", df.format(cgpa), metaLabelFont, metaValueFont, borderGray);
            
            // Add a full width row for status
            PdfPCell statusLabelCell = new PdfPCell(new Phrase("Result Status:", metaLabelFont));
            statusLabelCell.setBorder(Rectangle.BOX);
            statusLabelCell.setBorderColor(borderGray);
            statusLabelCell.setPadding(6);
            statusLabelCell.setColspan(2);
            summaryTable.addCell(statusLabelCell);

            PdfPCell statusValCell = new PdfPCell(new Phrase(passStatus, statusFont));
            statusValCell.setBorder(Rectangle.BOX);
            statusValCell.setBorderColor(borderGray);
            statusValCell.setPadding(6);
            statusValCell.setColspan(2);
            statusValCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            summaryTable.addCell(statusValCell);

            document.add(summaryTable);

            // 5. Signatures Placeholder Row
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{1f, 1f});
            signTable.setSpacingAfter(15);

            PdfPCell sig1 = new PdfPCell(new Paragraph("___________________________\nStudent Signature", metaValueFont));
            sig1.setBorder(Rectangle.NO_BORDER);
            sig1.setHorizontalAlignment(Element.ALIGN_LEFT);
            signTable.addCell(sig1);

            PdfPCell sig2 = new PdfPCell(new Paragraph("___________________________\nController of Examinations", metaValueFont));
            sig2.setBorder(Rectangle.NO_BORDER);
            sig2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signTable.addCell(sig2);

            document.add(signTable);

            // Footer Note
            Paragraph footerNote = new Paragraph("Note: This is a computer-generated official grade sheet, valid only under the seal of Exemplar Institute of Technology.", footerFont);
            footerNote.setAlignment(Element.ALIGN_CENTER);
            footerNote.setSpacingBefore(15);
            document.add(footerNote);

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while generating PDF report: " + e.getMessage(), e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valCell = new PdfPCell(new Phrase(value != null ? value : "", valFont));
        valCell.setBorder(Rectangle.NO_BORDER);
        valCell.setPadding(4);
        table.addCell(valCell);
    }

    private void addTableHeader(PdfPTable table, String text, Color background, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(Color.WHITE);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment, Color borderColor, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(borderColor);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        // Add zebra striping or default bg
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font labelFont, Font valFont, Color borderColor) {
        PdfPCell cell = new PdfPCell(new Phrase(label, labelFont));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(borderColor);
        cell.setPadding(6);
        table.addCell(cell);

        PdfPCell valCell = new PdfPCell(new Phrase(value, valFont));
        valCell.setBorder(Rectangle.BOX);
        valCell.setBorderColor(borderColor);
        valCell.setPadding(6);
        valCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(valCell);
    }
}
