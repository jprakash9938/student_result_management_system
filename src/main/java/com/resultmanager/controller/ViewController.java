package com.resultmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login.html")
    public String login() {
        return "login";
    }

    @GetMapping("/admin-dashboard.html")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/add-teacher.html")
    public String addTeacher() {
        return "add-teacher";
    }

    @GetMapping("/students.html")
    public String students() {
        return "students";
    }

    @GetMapping("/add-student.html")
    public String addStudent() {
        return "add-student";
    }

    @GetMapping("/teacher-dashboard.html")
    public String teacherDashboard() {
        return "teacher-dashboard";
    }

    @GetMapping("/subjects.html")
    public String subjects() {
        return "subjects";
    }

    @GetMapping("/marks.html")
    public String marks() {
        return "marks";
    }

    @GetMapping("/upload-excel.html")
    public String uploadExcel() {
        return "upload-excel";
    }

    @GetMapping("/results.html")
    public String results() {
        return "results";
    }

    @GetMapping("/student-dashboard.html")
    public String studentDashboard() {
        return "student-dashboard";
    }

    @GetMapping("/student-result.html")
    public String studentResult() {
        return "student-result";
    }
}
