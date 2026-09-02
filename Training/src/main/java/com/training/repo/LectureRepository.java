package com.training.repo;

import com.training.entity.Course;
import com.training.entity.Faculty;
import com.training.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    Optional<Lecture> findByLectureCode(String lectureCode);
    long countByCourse(Course course);
    List<Lecture> findByCourse(Course course);
    List<Lecture> findByCourseIn(List<Course> courses);
    List<Lecture> findByCourseAndFaculty(Course course, Faculty faculty);
}
