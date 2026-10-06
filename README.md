# Student Result Management System

A full-stack academic result management web application built with **Spring Boot 3.3**, **Thymeleaf**, **Spring Security**, and **PostgreSQL**. Deployed on **Render** using Docker containers.

**Live URL**: [https://student-result-management-h02j.onrender.com](https://student-result-management-h02j.onrender.com)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Architecture](#project-architecture)
- [Database Schema](#database-schema)
- [User Roles & Access Control](#user-roles--access-control)
- [REST API Endpoints](#rest-api-endpoints)
- [Pages & UI Flow](#pages--ui-flow)
- [Grading System](#grading-system)
- [Project Structure](#project-structure)
- [Configuration & Environment Variables](#configuration--environment-variables)
- [Local Development Setup](#local-development-setup)
- [Deployment on Render](#deployment-on-render)
- [Security Features](#security-features)

---

## Overview

The Student Result Management System is a web-based portal that allows educational institutions to manage student academic records digitally. It supports three user roles — **Admin**, **Teacher**, and **Student** — each with distinct dashboards and capabilities.

### How the System Works

1. **Admin** creates Teacher accounts and registers Students into the system.
2. **Teachers** create Subjects, enter marks for students (saved as DRAFT), and publish results when verified.
3. **Students** log in to view their published results, semester-wise grades, SGPA, and CGPA on a personalized dashboard.
4. Results go through a **DRAFT → PUBLISHED** workflow to ensure accuracy before students can see them.

---

## Features

- **Role-Based Access Control (RBAC)**: Admin, Teacher, Student roles with distinct permissions.
- **Result Workflow**: Two-stage (Draft → Published) result lifecycle.
- **SGPA & CGPA Calculation**: Automatic computation based on credit-weighted grade points.
- **Bulk Upload via Excel**: Teachers can upload student data and marks via `.xlsx` files.
- **PDF Transcript Generation**: Students can download semester transcripts as PDF.
- **Responsive UI**: Glassmorphism design with dark/light theme toggle, fully responsive on mobile.
- **CSRF Protection**: Cookie-based CSRF tokens on all state-changing requests.
- **Session Security**: HttpOnly, SameSite=Lax session cookies with configurable Secure flag.

---

## Technology Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.2 |
| **Frontend** | Thymeleaf, Vanilla JavaScript, CSS3 |
| **Security** | Spring Security 6, BCrypt password hashing, CSRF tokens |
| **Database** | PostgreSQL (via Spring Data JPA / Hibernate) |
| **Excel Processing** | Apache POI 5.2.5 |
| **PDF Generation** | OpenPDF 1.3.40 |
| **Environment Config** | spring-dotenv 4.0.0 (loads `.env` file) |
| **Deployment** | Docker (multi-stage build), Render |
| **Font/Icons** | Inter (Google Fonts), FontAwesome 6 |

---

## Project Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Browser (Client)                    │
│    Thymeleaf HTML Pages + Vanilla JS + CSS              │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP Requests
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Spring Security                       │
│   CSRF Filter → Auth Filter → Role Authorization        │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                REST Controllers                         │
│  AuthController │ StudentController │ TeacherController  │
│  SubjectController │ ResultController │ ViewController   │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Service Layer                          │
│  AuthService │ StudentService │ TeacherService           │
│  SubjectService │ ResultService │ ExcelUploadService     │
│  PdfGenerationService                                   │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│               Repository Layer (JPA)                    │
│  UserRepository │ StudentRepository                     │
│  SubjectRepository │ ResultRepository                   │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   PostgreSQL Database                   │
│     users │ students │ subjects │ results               │
└─────────────────────────────────────────────────────────┘
```

---

## Database Schema

### Entity Relationship

```
users (1) ──── (1) students
students (1) ──── (N) results
subjects (1) ──── (N) results
```

### Tables

#### `users`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary Key, Auto-increment |
| username | VARCHAR(50) | Unique, Not Null |
| password | VARCHAR | Not Null (BCrypt hashed) |
| role | VARCHAR(20) | ROLE_ADMIN / ROLE_TEACHER / ROLE_STUDENT |
| full_name | VARCHAR | Not Null |
| email | VARCHAR | Unique |

#### `students`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary Key, Auto-increment |
| roll_number | VARCHAR(50) | Unique, Not Null |
| department | VARCHAR(100) | Not Null |
| current_semester | INTEGER | Not Null |
| user_id | BIGINT | Foreign Key → users(id) |

#### `subjects`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary Key, Auto-increment |
| code | VARCHAR(20) | Unique, Not Null |
| name | VARCHAR(150) | Not Null |
| department | VARCHAR(100) | Not Null |
| semester | INTEGER | 1–8, Not Null |
| credits | INTEGER | 1–10, Not Null |

#### `results`

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary Key, Auto-increment |
| student_id | BIGINT | Foreign Key → students(id) |
| subject_id | BIGINT | Foreign Key → subjects(id) |
| semester | INTEGER | 1–8, Not Null |
| marks | DOUBLE | 0–100, Not Null |
| grade | VARCHAR(5) | Calculated (A+, A, B, C, D, F) |
| status | VARCHAR(20) | DRAFT / PUBLISHED |

> **Unique Constraint**: (student_id, subject_id, semester) — prevents duplicate entries.

---

## User Roles & Access Control

| Role | Capabilities |
|---|---|
| **ROLE_ADMIN** | Create/delete teacher accounts, register/manage students, view admin dashboard |
| **ROLE_TEACHER** | Manage subjects, enter/update/delete marks, upload Excel, publish results, view teacher dashboard |
| **ROLE_STUDENT** | View personal dashboard, view published results, download PDF transcript |

### Access Matrix

| Page / API | Admin | Teacher | Student |
|---|---|---|---|
| Landing Page (`/`) | ✅ | ✅ | ✅ |
| Login Page | ✅ | ✅ | ✅ |
| Admin Dashboard | ✅ | ❌ | ❌ |
| Teacher Dashboard | ❌ | ✅ | ❌ |
| Student Dashboard | ❌ | ❌ | ✅ |
| Manage Students | ✅ | ✅ | ❌ |
| Manage Subjects | ❌ | ✅ | ❌ |
| Enter Marks | ❌ | ✅ | ❌ |
| Upload Excel | ❌ | ✅ | ❌ |
| Publish Results | ❌ | ✅ | ❌ |
| View Own Results | ❌ | ❌ | ✅ |
| `/api/teachers/**` | ✅ | ❌ | ❌ |
| `/api/students/**` | ✅ | ✅ | ❌ |
| `/api/subjects/**` | ❌ | ✅ | ❌ |
| `/api/results/**` | ❌ | ✅ | ❌ |
| `/api/results/student/**` | ❌ | ❌ | ✅ |

---

## REST API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/login` | Login with username & password (form-urlencoded) |
| POST | `/api/auth/logout` | Logout and invalidate session |
| GET | `/api/auth/me` | Get current authenticated user info |

### Teachers (`/api/teachers`) — Admin only

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/teachers` | List all teachers |
| POST | `/api/teachers` | Create a new teacher account |
| DELETE | `/api/teachers/{id}` | Delete a teacher |

### Students (`/api/students`) — Admin & Teacher

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/students` | List all students (with optional filters) |
| GET | `/api/students/{id}` | Get student by ID |
| POST | `/api/students` | Register a new student |
| PUT | `/api/students/{id}` | Update student details |
| DELETE | `/api/students/{id}` | Delete a student |

### Subjects (`/api/subjects`) — Teacher

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/subjects` | List all subjects (with optional filters) |
| GET | `/api/subjects/{id}` | Get subject by ID |
| POST | `/api/subjects` | Create a new subject |
| PUT | `/api/subjects/{id}` | Update a subject |
| DELETE | `/api/subjects/{id}` | Delete a subject |

### Results (`/api/results`) — Teacher

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/results` | List all results |
| GET | `/api/results/stats` | Get dashboard statistics |
| POST | `/api/results` | Enter marks for a student (DRAFT) |
| PUT | `/api/results/{id}` | Update marks |
| DELETE | `/api/results/{id}` | Delete a result record |
| POST | `/api/results/publish` | Publish selected draft results |
| POST | `/api/results/upload` | Bulk upload marks via Excel file |
| GET | `/api/results/student/{id}` | Get results for a student |
| GET | `/api/results/student/{id}/dashboard` | Get student dashboard data (SGPA, CGPA) |
| GET | `/api/results/student/{id}/pdf` | Download PDF transcript |

---

## Pages & UI Flow

### Landing Page (`/`)

Two-portal front page with cards for **Admin Portal** and **Teacher/Student Portal**, both redirecting to the unified login page.

### Login Page (`/login.html`)

Secure login form. After authentication, users are redirected based on their role:

- `ROLE_ADMIN` → `/admin-dashboard.html`
- `ROLE_TEACHER` → `/teacher-dashboard.html`
- `ROLE_STUDENT` → `/student-dashboard.html`

### Admin Dashboard (`/admin-dashboard.html`)

- Quick shortcuts to create teachers and register students
- Table of all registered teachers with delete capability

### Teacher Dashboard (`/teacher-dashboard.html`)

- Stats overview: Total Students, Total Subjects, Published Results, Pending Drafts
- Quick shortcuts to Enter Marks, Upload Excel, Review & Publish

### Student Dashboard (`/student-dashboard.html`)

- Personal profile info (name, roll number, department, semester)
- CGPA display
- Semester-wise SGPA breakdown
- Published results table

---

## Grading System

| Marks Range | Grade | Grade Point |
|---|---|---|
| 90 – 100 | A+ | 10.0 |
| 80 – 89 | A | 9.0 |
| 70 – 79 | B | 8.0 |
| 60 – 69 | C | 7.0 |
| 50 – 59 | D | 6.0 |
| 0 – 49 | F | 0.0 |

**SGPA** = Σ(Grade Point × Credits) / Σ(Credits) — per semester

**CGPA** = Σ(Grade Point × Credits) / Σ(Credits) — across all semesters

---

## Project Structure

```
student-result-management/
├── .env                          # Local environment variables (DB credentials)
├── .gitignore                    # Git ignore rules (.env, target/, IDE files)
├── Dockerfile                    # Multi-stage Docker build for Render
├── render.yaml                   # Render Blueprint (auto-provision DB + Web Service)
├── pom.xml                       # Maven dependencies & build config
├── RENDER_DEPLOYMENT.md          # Deployment instructions
│
└── src/main/
    ├── java/com/resultmanager/
    │   ├── StudentResultApplication.java    # Spring Boot main class
    │   │
    │   ├── config/
    │   │   ├── SecurityConfig.java          # Spring Security filter chain & CSRF
    │   │   ├── CustomUserDetailsService.java # Loads users from DB for auth
    │   │   └── DatabaseSeeder.java          # Seeds default admin on first startup
    │   │
    │   ├── entity/
    │   │   ├── User.java                    # User entity (username, password, role)
    │   │   ├── Student.java                 # Student entity (rollNumber, dept, semester)
    │   │   ├── Subject.java                 # Subject entity (code, name, credits)
    │   │   ├── Result.java                  # Result entity (marks, grade, status)
    │   │   ├── Role.java                    # Enum: ROLE_ADMIN, ROLE_TEACHER, ROLE_STUDENT
    │   │   └── ResultStatus.java            # Enum: DRAFT, PUBLISHED
    │   │
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── StudentRepository.java
    │   │   ├── SubjectRepository.java
    │   │   └── ResultRepository.java
    │   │
    │   ├── service/
    │   │   ├── AuthService.java             # Authentication helper
    │   │   ├── StudentService.java          # Student CRUD operations
    │   │   ├── TeacherService.java          # Teacher CRUD operations
    │   │   ├── SubjectService.java          # Subject CRUD operations
    │   │   ├── ResultService.java           # Marks entry, grading, SGPA/CGPA
    │   │   ├── ExcelUploadService.java      # Apache POI Excel parsing
    │   │   └── PdfGenerationService.java    # OpenPDF transcript generation
    │   │
    │   ├── controller/
    │   │   ├── AuthController.java          # /api/auth endpoints
    │   │   ├── StudentController.java       # /api/students endpoints
    │   │   ├── TeacherController.java       # /api/teachers endpoints
    │   │   ├── SubjectController.java       # /api/subjects endpoints
    │   │   ├── ResultController.java        # /api/results endpoints
    │   │   └── ViewController.java          # Thymeleaf page routing
    │   │
    │   ├── dto/                             # Data Transfer Objects
    │   │   ├── StudentDto.java
    │   │   ├── TeacherDto.java
    │   │   ├── SubjectDto.java
    │   │   ├── ResultDto.java
    │   │   ├── ResultEntryDto.java
    │   │   ├── StudentDashboardDto.java
    │   │   ├── DashboardStatsDto.java
    │   │   └── ExcelUploadResponse.java
    │   │
    │   └── exception/
    │       ├── ResourceNotFoundException.java
    │       ├── DuplicateResourceException.java
    │       ├── InvalidDataException.java
    │       └── GlobalExceptionHandler.java
    │
    └── resources/
        ├── application.properties           # Spring Boot config (env var references)
        ├── static/
        │   ├── css/style.css                # Full design system (light/dark theme)
        │   └── js/app.js                    # Shared JS utilities (API calls, toasts)
        └── templates/
            ├── fragments.html               # Shared head, sidebar, header fragments
            ├── index.html                   # Landing page (2-portal)
            ├── login.html                   # Login form
            ├── admin-dashboard.html         # Admin dashboard
            ├── add-teacher.html             # Create teacher form
            ├── students.html                # Student list
            ├── add-student.html             # Add/edit student form
            ├── teacher-dashboard.html       # Teacher dashboard with stats
            ├── subjects.html                # Subject management
            ├── marks.html                   # Enter marks form
            ├── upload-excel.html            # Excel drag-and-drop upload
            ├── results.html                 # Review & publish results
            ├── student-dashboard.html       # Student profile + CGPA
            └── student-result.html          # Semester transcript view
```

---

## Configuration & Environment Variables

All sensitive configuration is stored in the `.env` file (excluded from Git via `.gitignore`).

The `spring-dotenv` library automatically loads `.env` values into Spring's property resolver.

### `.env` File

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=result_db
DB_USER=postgres
DB_PASSWORD=postgres
SESSION_COOKIE_SECURE=false
```

### `application.properties` Key Mappings

| Property | Environment Variable | Default |
|---|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` or `DB_HOST`/`DB_PORT`/`DB_NAME` | `jdbc:postgresql://localhost:5432/result_db` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` or `DB_USER` | `postgres` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` or `DB_PASSWORD` | `postgres` |
| `server.port` | `PORT` | `8080` |
| `server.servlet.session.cookie.secure` | `SESSION_COOKIE_SECURE` | `false` |

---

## Local Development Setup

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 15+

### Steps

1. **Clone the repository**:

   ```bash
   git clone https://github.com/Satyabrata-123/result.git
   cd result
   ```

2. **Create PostgreSQL database**:

   ```sql
   CREATE DATABASE result_db;
   ```

3. **Configure `.env`** file in project root with your local PostgreSQL credentials.

4. **Run the application**:

   ```bash
   mvn spring-boot:run
   ```

5. **Access the application** at `http://localhost:8080`

6. Hibernate will automatically create all tables on first startup (`ddl-auto=update`).

---

## Deployment on Render

The application is deployed using **Render Blueprints** with Docker.

### Automatic Deployment (Blueprint)

1. Push code to GitHub.
2. Go to Render Dashboard → **New +** → **Blueprint**.
3. Connect your repository. Render auto-detects `render.yaml`.
4. Click **Apply** — Render provisions PostgreSQL + Web Service automatically.

### Infrastructure

- **Web Service**: Docker container (Java 21 Alpine) built from `Dockerfile`
- **Database**: Render Managed PostgreSQL (Free tier)
- **Auto-deploy**: Render rebuilds on every `git push` to `main`

See [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md) for detailed manual setup instructions.

---

## Security Features

| Feature | Implementation |
|---|---|
| **Password Hashing** | BCrypt via Spring Security `PasswordEncoder` |
| **CSRF Protection** | Cookie-based CSRF tokens (CookieCsrfTokenRepository) on all POST/PUT/DELETE |
| **Session Management** | Server-side sessions with HttpOnly, SameSite=Lax cookies |
| **Role-Based Authorization** | Spring Security `authorizeHttpRequests` with role checks per endpoint |
| **Static Asset Security** | `WebSecurityCustomizer` bypasses filter chain for `/css/**`, `/js/**` |
| **Credential Protection** | All secrets in `.env` file excluded from version control |
| **Input Validation** | Jakarta Bean Validation annotations on all entities and DTOs |
| **Error Handling** | Global exception handler returns structured JSON errors, no stack traces exposed |
