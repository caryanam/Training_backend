package com.training.service;

import com.training.dto.responce.StudentCourseResponseDTO;
import com.training.dto.responce.StudentLectureResponseDTO;
import com.training.dto.responce.StudentMeetingResponseDTO;
import com.training.dto.responce.DummyPaymentResponseDTO;

import java.util.List;

public interface StudentPortalService {
    List<StudentCourseResponseDTO> getEnrolledCourses(String studentEmail);
    StudentCourseResponseDTO getEnrolledCourseDetail(String studentEmail, String courseId);
    List<StudentLectureResponseDTO> getEnrolledLectures(String studentEmail);
    List<StudentMeetingResponseDTO> getUpcomingMeetings(String studentEmail);
    List<DummyPaymentResponseDTO> getMyPayments(String studentEmail);
}
