package com.sss.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Audio {
    private String character;
    private String text;
    private String emotion;
    private String type;
    private Map<String, Integer> emotionParams;
    private String audioUrl;
} 