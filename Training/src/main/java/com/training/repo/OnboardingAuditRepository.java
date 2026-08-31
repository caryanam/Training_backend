package com.training.repo;

import com.training.entity.Course;
import com.training.entity.OnboardingAudit;
import com.training.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingAuditRepository extends JpaRepository<OnboardingAudit, Long> {
    Optional<OnboardingAudit> findByOnboardingCode(String onboardingCode);
    List<OnboardingAudit> findByStudent(User student);
    List<OnboardingAudit> findByStudentAndCourse(User student, Course course);
}
