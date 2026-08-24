package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.CreateCourseRequest;
import com.training.dto.request.UpdateCourseRequest;
import com.training.dto.responce.CourseResponseDTO;
import com.training.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CourseController {

    private final CourseService courseService;

    /**
     * GET /api/v1/admin/courses
     * Returns all courses with plans, lecture count, and active student count.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponseDTO>>> getAllCourses() {
        List<CourseResponseDTO> data = courseService.getAllCourses();
        return ResponseEntity.ok(new ApiResponse<>(true, "Courses fetched successfully.", data));
    }

    /**
     * GET /api/v1/admin/courses/{id}
     * Returns a single course by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> getCourseById(@PathVariable Long id) {
        CourseResponseDTO data = courseService.getCourseById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course fetched successfully.", data));
    }

    /**
     * POST /api/v1/admin/courses
     * Creates a new course with pricing plans atomically.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponseDTO>> createCourse(
            @Valid @RequestBody CreateCourseRequest dto) {
        CourseResponseDTO data = courseService.createCourse(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Course created successfully.", data));
    }

    /**
     * PUT /api/v1/admin/courses/{id}
     * Updates course fields and replaces pricing plans if provided.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest dto) {
        CourseResponseDTO data = courseService.updateCourse(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course updated successfully.", data));
    }

    /**
     * DELETE /api/v1/admin/courses/{id}
     * Soft-deletes if enrolled students/lectures exist; hard-deletes otherwise.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course deleted successfully.", null));
    }
}