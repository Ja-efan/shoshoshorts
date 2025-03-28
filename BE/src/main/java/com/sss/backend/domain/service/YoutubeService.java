package com.sss.backend.domain.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.sss.backend.api.dto.VideoMetadata;
import com.sss.backend.api.dto.VideoUploadResponse;
import com.sss.backend.domain.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Service
public class YoutubeService {

    @Value("${youtube.application.name}")
    private String applicationName;

    private final VideoRepository videoRepository;

    public YoutubeService(VideoRepository videoRepository){
        this.videoRepository = videoRepository;
    }

    // 비동기 방식의 업로드 메서드 (Controller에서 호출)
    public Mono<VideoUploadResponse> uploadVideo(String accessToken, String videoUrl, VideoMetadata metadata) {
        return Mono.fromCallable(() -> {
            try {
                if (videoUrl == null || videoUrl.isEmpty()) {
                    throw new RuntimeException("비디오 URL이 필요합니다.");
                }

                // 제공된 URL에서 임시 파일로 다운로드
                File tempFile = File.createTempFile("youtube-upload-", ".tmp");
                downloadFromUrl(videoUrl, tempFile);

                try {
                    // 동기식 업로드 메서드 호출
                    String youtubeVideoId = uploadVideoToYoutube(
                            tempFile,
                            metadata.getTitle(),
                            metadata.getDescription(),
                            metadata.getTags(),
                            metadata.getPrivacyStatus(),
                            metadata.getCategoryId(),
                            accessToken
                    );

                    // API 명세에 맞는 응답 생성
                    VideoUploadResponse response = new VideoUploadResponse();
                    response.setMessage("비디오 업로드 성공");
                    response.setVideoId(youtubeVideoId);
                    response.setVideoUrl("https://www.youtube.com/watch?v=" + youtubeVideoId);

                    return response;

                } catch (Exception e) {
                    throw e;
                } finally {
                    // 임시 파일 삭제
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("비디오 업로드 실패: " + e.getMessage(), e);
            }
        });
    }


    // URL에서 파일 다운로드하는 메서드
    private void downloadFromUrl(String urlStr, File destination) throws IOException {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(destination)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }



    // 실제 YouTube API 호출하는 내부 메서드
    private String uploadVideoToYoutube(File videoFile, String title, String description,
                                        String tags, String privacyStatus, String categoryId,
                                        String accessToken) throws IOException {

        // 인증 정보 생성
        Credential credential = new GoogleCredential().setAccessToken(accessToken);

        // YouTube 서비스 객체 생성
        YouTube youtube = new YouTube.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(applicationName)
                .build();

        // 동영상 메타데이터 설정
        Video videoMetadata = new Video();

        // 기본 정보 설정
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(title != null ? title : "Untitled");
        snippet.setDescription(description != null ? description : "");

        // 태그 설정 (있는 경우)
        if (tags != null && !tags.isEmpty()) {
            snippet.setTags(Arrays.asList(tags.split(",")));
        }

        // 카테고리 설정
        snippet.setCategoryId(categoryId != null ? categoryId : "22"); // 기본값 22 (People & Blogs)

        // 개인정보 설정
        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus(privacyStatus != null ? privacyStatus : "private"); // 기본값 private

        videoMetadata.setSnippet(snippet);
        videoMetadata.setStatus(status);

        // 업로드 요청 생성
        YouTube.Videos.Insert videoInsert = youtube.videos()
                .insert(Collections.singletonList("snippet,status"), videoMetadata,
                        new FileContent("video/*", videoFile));

        // 업로드 실행 및 결과 받기
        Video uploadedVideo = videoInsert.execute();

        // 비디오 ID 반환
        return uploadedVideo.getId();
    }


    // 썸네일 설정 (선택적 기능)
    public void setThumbnail(String videoId, File thumbnailFile, String accessToken) throws IOException {
        // 인증 정보 생성
        Credential credential = new GoogleCredential().setAccessToken(accessToken);

        // YouTube 서비스 객체 생성
        YouTube youtube = new YouTube.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(applicationName)
                .build();

        // 썸네일 설정 요청 생성
        YouTube.Thumbnails.Set thumbnailSet = youtube.thumbnails()
                .set(videoId, new FileContent("image/jpeg", thumbnailFile));

        // 요청 실행
        thumbnailSet.execute();
    }

}