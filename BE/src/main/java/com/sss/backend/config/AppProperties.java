package com.sss.backend.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    // 이미지 API URL
    private String imageApiUrl;

    // 이미지 저장 경로
    private String imageStoragePath;


}
