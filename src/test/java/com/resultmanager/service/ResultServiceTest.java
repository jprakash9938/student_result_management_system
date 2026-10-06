package com.resultmanager.service;

import com.resultmanager.entity.Result;
import com.resultmanager.entity.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResultServiceTest {

    @InjectMocks
    private ResultService resultService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCalculateGrade() {
        assertEquals("A+", resultService.calculateGrade(95.0));
        assertEquals("A+", resultService.calculateGrade(90.0));
        
        assertEquals("A", resultService.calculateGrade(89.5));
        assertEquals("A", resultService.calculateGrade(80.0));
        
        assertEquals("B", resultService.calculateGrade(75.0));
        assertEquals("B", resultService.calculateGrade(70.0));
        
        assertEquals("C", resultService.calculateGrade(65.0));
        assertEquals("C", resultService.calculateGrade(60.0));
        
        assertEquals("D", resultService.calculateGrade(55.0));
        assertEquals("D", resultService.calculateGrade(50.0));
        
        assertEquals("F", resultService.calculateGrade(49.9));
        assertEquals("F", resultService.calculateGrade(0.0));
        assertEquals("F", resultService.calculateGrade(null));
    }

    @Test
    public void testCalculateSgpaFromResults() throws Exception {
        // We test the private calculateSgpaFromResults method using reflection
        Method method = ResultService.class.getDeclaredMethod("calculateSgpaFromResults", List.class);
        method.setAccessible(true);

        Subject sub1 = Subject.builder().credits(4).build();
        Subject sub2 = Subject.builder().credits(3).build();

        Result r1 = Result.builder().subject(sub1).grade("A+").build(); // GP = 10.0
        Result r2 = Result.builder().subject(sub2).grade("B").build();  // GP = 8.0
        // Expected SGPA = ((10 * 4) + (8 * 3)) / (4 + 3) = (40 + 24) / 7 = 64 / 7 = 9.14

        List<Result> results = Arrays.asList(r1, r2);
        double sgpa = (double) method.invoke(resultService, results);
        
        assertEquals(9.14, sgpa, 0.01);
    }

    @Test
    public void testCalculateCgpaFromResults() throws Exception {
        // We test the private calculateCgpaFromResults method using reflection
        Method method = ResultService.class.getDeclaredMethod("calculateCgpaFromResults", List.class);
        method.setAccessible(true);

        Subject sub1 = Subject.builder().credits(4).build();
        Subject sub2 = Subject.builder().credits(3).build();
        Subject sub3 = Subject.builder().credits(2).build();

        Result r1 = Result.builder().subject(sub1).grade("A").build();  // GP = 9.0
        Result r2 = Result.builder().subject(sub2).grade("B").build();  // GP = 8.0
        Result r3 = Result.builder().subject(sub3).grade("F").build();  // GP = 0.0
        // Expected CGPA = ((9 * 4) + (8 * 3) + (0 * 2)) / (4 + 3 + 2) = (36 + 24 + 0) / 9 = 60 / 9 = 6.67

        List<Result> results = Arrays.asList(r1, r2, r3);
        double cgpa = (double) method.invoke(resultService, results);

        assertEquals(6.67, cgpa, 0.01);
    }
}
