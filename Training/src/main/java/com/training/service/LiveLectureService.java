package com.training.service;

import com.training.dto.responce.LiveLectureJoinResponseDTO;
import com.training.dto.responce.LiveLectureStartResponseDTO;
import com.training.dto.responce.LiveLectureStatusResponseDTO;

public interface LiveLectureService {
    LiveLectureStartResponseDTO startLiveLecture(String lectureId, String facultyEmail);
    LiveLectureJoinResponseDTO joinLiveLecture(String lectureId, String studentEmail);
    LiveLectureStatusResponseDTO endLiveLecture(String lectureId, String facultyEmail);
    void leaveLiveLecture(String lectureId, String studentEmail);
    LiveLectureStatusResponseDTO getLiveStatus(String lectureId);
    void heartbeat(String lectureId, String userEmail);
}
