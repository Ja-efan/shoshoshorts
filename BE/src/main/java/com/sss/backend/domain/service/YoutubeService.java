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
import com.sss.backend.config.S3Config;
import com.sss.backend.domain.repository.StoryRepository;
import com.sss.backend.domain.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Service
public class YoutubeService {

    @Value("${youtube.application.name}")
    private String applicationName;

    private final VideoRepository videoRepository;
    private final S3Config s3Config;
    private final StoryRepository storyRepository;

    public YoutubeService(VideoRepository videoRepository, S3Config s3Config, StoryRepository storyRepository){
        this.videoRepository = videoRepository;
        this.s3Config = s3Config;
        this.storyRepository = storyRepository;
    }

    // 비동기 방식의 업로드 메서드 (Controller에서 호출)
    public Mono<VideoUploadResponse> uploadVideo(String accessToken, String storyId, VideoMetadata metadata, Long userId) {
        return Mono.fromCallable(() -> {
            try {

                //user가 일치하는 지 확인

                Optional<com.sss.backend.domain.entity.Video> videoOpt = videoRepository.findByStoryId(Long.parseLong(storyId));

                if (!videoOpt.isPresent()) {
                    throw new RuntimeException("해당 스토리 ID에 해당하는 비디오가 없습니다.");
                }

                com.sss.backend.domain.entity.Video video = videoOpt.get();

                // 2. 비디오의 소유자 확인 (Story의 User ID와 로그인한 사용자 ID 비교)
                if (!video.getStory().getUser().getId().equals(userId)) {
                    throw new RuntimeException("로그인한 사용자에 해당하는 storyId가 아닙니다.");
                }

                //storyId를 통해서 videoUrl 찾기
                String videoUrl = videoRepository.findVideoUrlByStoryId(Long.parseLong(storyId));

                if (videoUrl == null || videoUrl.isEmpty()) {
                    throw new RuntimeException("비디오 URL이 필요합니다.");
                }

                // 임시로 동영상 파일을 담을 file 공간 생성
                File tempFile = File.createTempFile("youtube-upload-", ".tmp");

                try {

                    // S3 URL에서 키 추출
                    String s3key = s3Config.extractS3KeyFromUrl(videoUrl);

                    // S3에서 파일 다운로드
                    s3Config.downloadFromS3(s3key,tempFile.getAbsolutePath());

                    //기본값 다 넣어주기
                    setDefaultMetadata(metadata, storyId);

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


    // 실제 YouTube API 호출하는 내부 메서드
    private String uploadVideoToYoutube(File videoFile, String title, String description,
                                        String tags, String privacyStatus, String categoryId,
                                        String accessToken) throws IOException {

        try {
            System.out.println("YouTube 업로드 시작 - 파일 크기: " + videoFile.length() + " bytes");
            System.out.println("사용 토큰 (부분): " + accessToken.substring(0, Math.min(10, accessToken.length())) + "...");

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
            status.setPrivacyStatus(privacyStatus != null ? privacyStatus : "public"); // 기본값 public

            videoMetadata.setSnippet(snippet);
            videoMetadata.setStatus(status);

            // 업로드 요청 생성
            YouTube.Videos.Insert videoInsert = youtube.videos()
                    .insert(Collections.singletonList("snippet,status"), videoMetadata,
                            new FileContent("video/*", videoFile));

            System.out.println("YouTube API 호출 전...");

            // 업로드 실행 및 결과 받기
            Video uploadedVideo = videoInsert.execute();

            System.out.println("YouTube API 호출 성공!");

            // 비디오 ID 반환
            return uploadedVideo.getId();
        } catch (IOException e) {
            System.out.println("YouTube 업로드 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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


    public void setDefaultMetadata(VideoMetadata metadata, String storyId){

        // 스토리 ID로부터 정보 가져오기
        String storyTitle = storyRepository.findTitleById(Long.parseLong(storyId));

        // 제목
        if (metadata.getTitle() == null || metadata.getTitle().isEmpty()) {
            metadata.setTitle(storyTitle != null ? storyTitle : "Untitled");
        }

        //설명
        if (metadata.getDescription() == null || metadata.getDescription().isEmpty()) {
            metadata.setDescription("#Shorts");
        } else {
            metadata.setDescription(metadata.getDescription() + " #Shorts");
        }

        //태그
        if (metadata.getTags() == null || metadata.getTags().isEmpty()) {
            metadata.setTags("shorts");
        }

        // 개인정보 설정
        if (metadata.getPrivacyStatus() == null || metadata.getPrivacyStatus().isEmpty()) {
            metadata.setPrivacyStatus("public");
        }

        // 카테고리 ID
        if (metadata.getCategoryId() == null || metadata.getCategoryId().isEmpty()) {
            metadata.setCategoryId("23");
        }
    }

}