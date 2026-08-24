package com.training.service;

import com.training.dto.request.CreateCourseRequest;
import com.training.dto.request.UpdateCourseRequest;
import com.training.dto.responce.CourseResponseDTO;

import java.util.List;

public interface CourseService {
    List<CourseResponseDTO> getAllCourses();
    CourseResponseDTO getCourseById(Long id);
    CourseResponseDTO createCourse(CreateCourseRequest dto);
    CourseResponseDTO updateCourse(Long id, UpdateCourseRequest dto);
    void deleteCourse(Long id);
}
