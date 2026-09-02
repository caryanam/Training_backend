package com.training.repo;

import com.training.entity.Course;
import com.training.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<Course> findByFaculty(Faculty faculty);
}
