package com.training.service;

import com.training.dto.request.CreateLectureDTO;
import com.training.dto.responce.LectureAccessResponseDTO;
import com.training.dto.responce.LectureResponseDTO;

public interface LectureService {
    LectureResponseDTO createLecture(CreateLectureDTO dto);
    LectureResponseDTO createLecture(CreateLectureDTO dto, String authenticatedEmail);
    LectureAccessResponseDTO getLectureAccess(String lectureId, String userEmail);
}
