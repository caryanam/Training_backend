package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.responce.DummyPaymentResponseDTO;
import com.training.dto.responce.StudentCourseResponseDTO;
import com.training.dto.responce.StudentLectureResponseDTO;
import com.training.dto.responce.StudentMeetingResponseDTO;
import com.training.service.StudentPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentPortalService studentPortalService;

    /**
     * GET /api/v1/student/courses
     * Returns ONLY courses where the student has an ACTIVE enrollment.
     */
    @GetMapping("/courses")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<ApiResponse<List<StudentCourseResponseDTO>>> getMyEnrolledCourses(Authentication authentication) {
        String studentEmail = authentication.getName();
        List<StudentCourseResponseDTO> data = studentPortalService.getEnrolledCourses(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrolled courses retrieved successfully.", data));
    }

    /**
     * GET /api/v1/student/courses/{courseId}
     * Returns course details only if student is enrolled in that specific course.
     */
    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<ApiResponse<StudentCourseResponseDTO>> getEnrolledCourseDetail(
            @PathVariable String courseId,
            Authentication authentication) {
        String studentEmail = authentication.getName();
        StudentCourseResponseDTO data = studentPortalService.getEnrolledCourseDetail(studentEmail, courseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrolled course details retrieved.", data));
    }

    /**
     * GET /api/v1/student/lectures
     * Returns lectures ONLY for courses in which the student has an ACTIVE enrollment.
     */
    @GetMapping("/lectures")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<ApiResponse<List<StudentLectureResponseDTO>>> getMyEnrolledLectures(Authentication authentication) {
        String studentEmail = authentication.getName();
        List<StudentLectureResponseDTO> data = studentPortalService.getEnrolledLectures(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrolled lectures retrieved successfully.", data));
    }

    /**
     * GET /api/v1/student/meetings
     * Returns upcoming live sessions / meetings ONLY for courses in which the student is enrolled.
     */
    @GetMapping("/meetings")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<ApiResponse<List<StudentMeetingResponseDTO>>> getMyUpcomingMeetings(Authentication authentication) {
        String studentEmail = authentication.getName();
        List<StudentMeetingResponseDTO> data = studentPortalService.getUpcomingMeetings(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Enrolled meetings retrieved successfully.", data));
    }

    /**
     * GET /api/v1/student/payments
     * Returns payment records for the authenticated student only.
     */
    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<ApiResponse<List<DummyPaymentResponseDTO>>> getMyPayments(Authentication authentication) {
        String studentEmail = authentication.getName();
        List<DummyPaymentResponseDTO> data = studentPortalService.getMyPayments(studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Student payments retrieved successfully.", data));
    }
}
