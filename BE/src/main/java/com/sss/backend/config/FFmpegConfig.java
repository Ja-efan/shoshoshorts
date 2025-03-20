package com.sss.backend.config;

import net.bramp.ffmpeg.FFmpeg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FFmpegConfig {
    
    @Value("${ffmpeg.path}")
    private String ffmpegPath;
    
    @Bean
    public FFmpeg ffmpeg() throws Exception {
        return new FFmpeg(ffmpegPath);
    }
} 