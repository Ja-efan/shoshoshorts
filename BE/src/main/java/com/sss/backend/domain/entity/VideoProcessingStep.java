package com.sss.backend.domain.entity;

public enum VideoProcessingStep {
    SCRIPT_PROCESSING("스크립트 처리 중"),
    VOICE_GENERATING("AI 음성 생성 중"),
    IMAGE_GENERATING("AI 이미지 생성 중"),
    VOICE_COMPLETED("AI 음성 생성 완료"),
    IMAGE_COMPLETED("AI 이미지 생성 완료"),
    VIDEO_RENDERING("영상 병합 중"),
    VIDEO_RENDER_COMPLETED("영상 병합 완료"),
    VIDEO_UPLOADING("영상 업로드 중");

    private final String description;

    VideoProcessingStep(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 