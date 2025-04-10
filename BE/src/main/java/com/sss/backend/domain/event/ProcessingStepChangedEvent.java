package com.sss.backend.domain.event;

import com.sss.backend.domain.entity.VideoProcessingStep;
import lombok.Getter;

/**
 * 비디오 처리 단계 변경 이벤트
 */
@Getter
public class ProcessingStepChangedEvent {
    
    private final String storyId;
    private final VideoProcessingStep step;
    
    public ProcessingStepChangedEvent(String storyId, VideoProcessingStep step) {
        this.storyId = storyId;
        this.step = step;
    }
} 