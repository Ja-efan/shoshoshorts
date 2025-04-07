package com.sss.backend.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VoiceZonosVectorDTO {
    private List<List<List<Float>>> speaker_tensor; // 3중 lsit
    private String s3_url;

}
