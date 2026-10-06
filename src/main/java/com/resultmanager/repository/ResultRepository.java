package com.resultmanager.repository;

import com.resultmanager.entity.Result;
import com.resultmanager.entity.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByStudentId(Long studentId);
    List<Result> findByStudentIdAndSemester(Long studentId, Integer semester);
    List<Result> findByStudentIdAndStatus(Long studentId, ResultStatus status);
    List<Result> findByStudentIdAndSemesterAndStatus(Long studentId, Integer semester, ResultStatus status);
    Optional<Result> findByStudentIdAndSubjectIdAndSemester(Long studentId, Long subjectId, Integer semester);
    long countByStatus(ResultStatus status);
}
