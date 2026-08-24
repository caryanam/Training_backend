package com.training.repo;

import com.training.entity.Course;
import com.training.entity.Enrollment;
import com.training.entity.User;
import com.training.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByEnrollmentCode(String enrollmentCode);
    List<Enrollment> findByStudentAndCourse(User student, Course course);
    Optional<Enrollment> findFirstByStudentAndCourseAndStatusOrderByExpiryDateDesc(User student, Course course, EnrollmentStatus status);
    long countByCourseAndStatus(Course course, EnrollmentStatus status);
    boolean existsByCourse(Course course);
}

