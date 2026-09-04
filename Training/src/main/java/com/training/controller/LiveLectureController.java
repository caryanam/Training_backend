package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.responce.LiveLectureJoinResponseDTO;
import com.training.dto.responce.LiveLectureStartResponseDTO;
import com.training.dto.responce.LiveLectureStatusResponseDTO;
import com.training.service.LiveLectureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lectures/{lectureId}/live")
@RequiredArgsConstructor
public class LiveLectureController {

    private final LiveLectureService liveLectureService;

    /**
     * POST /api/v1/lectures/{lectureId}/live/start
     * Faculty starts live streaming classroom. Generates LiveKit publisher token.
     */
    @PostMapping("/start")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LiveLectureStartResponseDTO>> startLiveLecture(
            @PathVariable String lectureId,
            Authentication authentication) {
        String facultyEmail = authentication.getName();
        LiveLectureStartResponseDTO data = liveLectureService.startLiveLecture(lectureId, facultyEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Live lecture session started successfully.", data));
    }

    /**
     * POST /api/v1/lectures/{lectureId}/live/join
     * Student joins live streaming classroom. Validates active enrollment & generates subscribe-only LiveKit token.
     */
    @PostMapping("/join")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LiveLectureJoinResponseDTO>> joinLiveLecture(
            @PathVariable String lectureId,
            Authentication authentication) {
        String studentEmail = authentication.getName();
        LiveLectureJoinResponseDTO data = liveLectureService.joinLiveLecture(lectureId, studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Successfully joined live lecture.", data));
    }

    /**
     * POST /api/v1/lectures/{lectureId}/live/end
     * Faculty ends live stream session. Terminates room & disconnects active participants.
     */
    @PostMapping("/end")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LiveLectureStatusResponseDTO>> endLiveLecture(
            @PathVariable String lectureId,
            Authentication authentication) {
        String facultyEmail = authentication.getName();
        LiveLectureStatusResponseDTO data = liveLectureService.endLiveLecture(lectureId, facultyEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Live lecture session ended.", data));
    }

    /**
     * POST /api/v1/lectures/{lectureId}/live/leave
     * Student voluntarily leaves live classroom.
     */
    @PostMapping("/leave")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> leaveLiveLecture(
            @PathVariable String lectureId,
            Authentication authentication) {
        String studentEmail = authentication.getName();
        liveLectureService.leaveLiveLecture(lectureId, studentEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Left live classroom."));
    }

    /**
     * GET /api/v1/lectures/{lectureId}/live/status
     * Returns current live status & active participant count for a lecture.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<LiveLectureStatusResponseDTO>> getLiveStatus(
            @PathVariable String lectureId) {
        LiveLectureStatusResponseDTO data = liveLectureService.getLiveStatus(lectureId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Live status retrieved.", data));
    }

    /**
     * POST /api/v1/lectures/{lectureId}/live/heartbeat
     * Updates participant heartbeat to detect stale sessions.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @PathVariable String lectureId,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        if (userEmail != null) {
            liveLectureService.heartbeat(lectureId, userEmail);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Heartbeat acknowledged."));
    }
}
