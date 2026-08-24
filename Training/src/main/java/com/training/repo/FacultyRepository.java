package com.training.repo;

import com.training.entity.Faculty;
import com.training.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByUser(User user);
    Optional<Faculty> findByUserEmail(String email);
    Optional<Faculty> findByFacultyCode(String facultyCode);
}
