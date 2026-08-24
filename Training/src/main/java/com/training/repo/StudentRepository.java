package com.training.repo;

import com.training.entity.Student;
import com.training.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    Optional<Student> findByUserEmail(String email);
    Optional<Student> findByStudentCode(String studentCode);
}
