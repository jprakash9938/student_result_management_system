package com.resultmanager.repository;

import com.resultmanager.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRollNumber(String rollNumber);
    boolean existsByRollNumber(String rollNumber);

    @Query("SELECT s FROM Student s WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:dept IS NULL OR :dept = '' OR s.department = :dept) AND " +
           "(:sem IS NULL OR s.currentSemester = :sem)")
    List<Student> searchStudents(
            @Param("search") String search,
            @Param("dept") String dept,
            @Param("sem") Integer sem
    );
}
