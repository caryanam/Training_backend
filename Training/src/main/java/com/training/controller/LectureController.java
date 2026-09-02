package com.training.controller;

import com.training.dto.ApiResponse;
import com.training.dto.request.CreateLectureDTO;
import com.training.dto.responce.LectureAccessResponseDTO;
import com.training.dto.responce.LectureResponseDTO;
import com.training.service.LectureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;

    @PostMapping
    public ResponseEntity<ApiResponse<LectureResponseDTO>> createLecture(
            @Valid @RequestBody CreateLectureDTO dto,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        LectureResponseDTO data = lectureService.createLecture(dto, userEmail);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Lecture created successfully.",
                        data
                ));
    }

    @GetMapping("/{lectureId}/access")
    public ResponseEntity<ApiResponse<LectureAccessResponseDTO>> getLectureAccess(
            @PathVariable String lectureId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        LectureAccessResponseDTO data = lectureService.getLectureAccess(lectureId, userEmail);

        if (!data.isHasAccess()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(
                            false,
                            data.getReason(),
                            data
                    ));
        }

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                data.getReason(),
                data
        ));
    }
}
