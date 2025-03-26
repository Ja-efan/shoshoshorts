package com.sss.backend.domain.service;

import com.sss.backend.api.dto.VideoListResponseDTO;
import com.sss.backend.api.dto.VideoStatusAllDTO;
import com.sss.backend.config.S3Config;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.sss.backend.domain.document.SceneDocument;
import com.sss.backend.domain.repository.SceneDocumentRepository;
import com.sss.backend.domain.entity.Story;
import com.sss.backend.domain.entity.Video;
import com.sss.backend.domain.repository.StoryRepository;
import com.sss.backend.domain.repository.VideoRepository;
import com.sss.backend.domain.entity.Video.VideoStatus;
import com.sss.backend.api.dto.VideoStatusResponseDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import jakarta.annotation.PostConstruct;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);
    private static final String TEMP_DIR = "/app/temp/videos";
    
    private final SceneDocumentRepository sceneDocumentRepository;
    private final S3Config s3Config;
    private final FFmpeg ffmpeg;
    private final StoryRepository storyRepository;
    private final VideoRepository videoRepository;
    
    @PostConstruct
    public void init() {
        createTempDir();
    }

    private void createTempDir() {
        File dir = new File(TEMP_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            logger.info("임시 디렉토리 생성: {} (성공: {})", TEMP_DIR, created);
            if (!created) {
                File parentDir = new File(dir.getParent());
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                dir.mkdirs();
            }
        }
    }

    public File mergeAudioFiles(List<String> audioUrls, String outputPath) {
        try {
            if (audioUrls.isEmpty()) {
                throw new RuntimeException("병합할 오디오 URL이 없습니다");
            }
            
            createTempDir();

            // 임시 출력 경로는 중간 작업에만 사용
            String tempOutputPath = TEMP_DIR + "/merged_" + UUID.randomUUID() + ".mp3";
            String cleanTempPath = tempOutputPath.replace("\"", "");
            String cleanOutputPath = outputPath.replace("\"", "");

            // 첫 번째 오디오 처리
            String firstS3Key = s3Config.extractS3KeyFromUrl(audioUrls.get(0));
            String firstPresignedUrl = s3Config.generatePresignedUrl(firstS3Key);

            FFmpegBuilder firstBuilder = new FFmpegBuilder()
                .setInput(firstPresignedUrl)
                .addExtraArgs("-y")
                .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls")
                .addOutput(cleanTempPath)
                .setAudioCodec("libmp3lame")
                .setFormat("mp3")
                .done();
                
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(firstBuilder).run();
            
            // 추가 오디오 파일 병합
            for (int i = 1; i < audioUrls.size(); i++) {
                String tempOutput = TEMP_DIR + "/temp_" + UUID.randomUUID() + ".mp3";
                String cleanTempOutput = tempOutput.replace("\"", "");

                String s3Key = s3Config.extractS3KeyFromUrl(audioUrls.get(i));
                String presignedUrl = s3Config.generatePresignedUrl(s3Key);

                FFmpegBuilder appendBuilder = new FFmpegBuilder()
                    .addInput(cleanTempPath)
                    .addInput(presignedUrl)
                    .addExtraArgs("-y")
                    .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls")
                    .addOutput(cleanTempOutput)
                    .addExtraArgs("-filter_complex", "[0:a][1:a]concat=n=2:v=0:a=1[outa]")
                    .addExtraArgs("-map", "[outa]")
                    .setAudioCodec("libmp3lame")
                    .setFormat("mp3")
                    .done();
                    
                executor.createJob(appendBuilder).run();
                Files.move(Paths.get(cleanTempOutput), Paths.get(cleanTempPath), StandardCopyOption.REPLACE_EXISTING);
            }

            // 최종 결과를 바로 outputPath에 생성
            if (!cleanTempPath.equals(cleanOutputPath)) {
                Files.move(Paths.get(cleanTempPath), Paths.get(cleanOutputPath), StandardCopyOption.REPLACE_EXISTING);
            }
            
            return new File(cleanOutputPath);
        } catch (Exception e) {
            logger.error("오디오 파일 병합 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오디오 파일 병합 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    public File createVideoFromImageAndAudio(String imageUrl, String audioPath, String outputPath) {
        try {
            createTempDir();

            String cleanOutputPath = outputPath.replace("\"", "");
            String tempAudioPath = TEMP_DIR + "/audio_" + UUID.randomUUID() + ".mp3";
            String cleanAudioPath = tempAudioPath.replace("\"", "");

            // 오디오 파일 복사 (필수 과정)
            Files.copy(Paths.get(audioPath), Paths.get(cleanAudioPath), StandardCopyOption.REPLACE_EXISTING);

            // S3 pre-signed URL 생성
            String imageS3Key = s3Config.extractS3KeyFromUrl(imageUrl);
            String presignedImageUrl = s3Config.generatePresignedUrl(imageS3Key);

            // 바로 최종 출력 경로에 생성
            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(presignedImageUrl)
                .addInput(cleanAudioPath)
                .addExtraArgs("-y")
                .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls")
                .addOutput(cleanOutputPath)
                .addExtraArgs("-filter_complex", 
                    "[0:v]scale=iw*min(1080/iw\\,1920/ih):ih*min(1080/iw\\,1920/ih)," +
                    "pad=1080:1920:(1080-iw*min(1080/iw\\,1920/ih))/2:(1920-ih*min(1080/iw\\,1920/ih))/2:white[v];" +
                    "[v][1:a]concat=n=1:v=1:a=1[outv][outa]")
                .addExtraArgs("-map", "[outv]")
                .addExtraArgs("-map", "[outa]")
                .setVideoCodec("libx264")
                .setAudioCodec("aac")
                .setFormat("mp4")
                .done();
                
            // 실행
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();
            
            // 임시 오디오 파일 삭제
            new File(cleanAudioPath).delete();

            return new File(cleanOutputPath);
        } catch (Exception e) {
            logger.error("이미지와 오디오 합성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("이미지와 오디오 합성 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    public File mergeVideos(List<String> videoPaths, String outputPath) {
        try {
            createTempDir();

            String cleanOutputPath = outputPath.replace("\"", "");

            // 비디오 파일을 임시 경로로 복사 (여러 파일 병합 위해 필요한 과정)
            List<String> tempVideoPaths = new ArrayList<>();
            for (int i = 0; i < videoPaths.size(); i++) {
                String tempVideoPath = TEMP_DIR + "/scene_video_" + i + "_" + UUID.randomUUID() + ".mp4";
                String cleanVideoPath = tempVideoPath.replace("\"", "");
                Files.copy(Paths.get(videoPaths.get(i)), Paths.get(cleanVideoPath), StandardCopyOption.REPLACE_EXISTING);
                tempVideoPaths.add(cleanVideoPath);
            }

            // 임시 파일 생성 (파일 목록)
            Path listFilePath = Paths.get(TEMP_DIR, "video_list_" + UUID.randomUUID() + ".txt");
            
            StringBuilder fileList = new StringBuilder();
            for (String videoPath : tempVideoPaths) {
                fileList.append("file '").append(videoPath).append("'\n");
            }
            
            Files.write(listFilePath, fileList.toString().getBytes());
            
            // 바로 최종 출력 경로에 생성
            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(listFilePath.toString())
                .addExtraArgs("-y")
                .addExtraArgs("-f", "concat")
                .addExtraArgs("-safe", "0")
                .addOutput(cleanOutputPath)
                .setVideoCodec("libx264")
                .setAudioCodec("aac")
                .setFormat("mp4")
                .done();
                
            // 실행
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();
            
            // 임시 파일 삭제
            Files.delete(listFilePath);
            for (String path : tempVideoPaths) {
                new File(path).delete();
            }

            return new File(cleanOutputPath);
        } catch (IOException e) {
            logger.error("비디오 병합 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("비디오 병합 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    public File createFinalVideo(String storyId, String outputPath) {
        try {
            String cleanOutputPath = outputPath.replace("\"", "");
            logger.info("스토리 ID {} 에 대한 비디오 생성 시작", storyId);
            
            // 스토리 조회 및 유효성 검사
            Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
            if (sceneDocumentOpt.isEmpty()) {
                throw new RuntimeException("스토리를 찾을 수 없음: " + storyId);
            }
            
            SceneDocument sceneDocument = sceneDocumentOpt.get();
            List<Map<String, Object>> scenes = sceneDocument.getSceneArr();
            
            if (scenes == null || scenes.isEmpty()) {
                throw new RuntimeException("스토리에 씬이 없음: " + storyId);
            }

            List<String> sceneVideoPaths = new ArrayList<>();
            
            // 각 Scene별로 처리
            for (int i = 0; i < scenes.size(); i++) {
                Map<String, Object> scene = scenes.get(i);
                logger.info("씬 처리 중 {}/{}", i + 1, scenes.size());
                
                // 씬 데이터 유효성 검사
                if (scene.get("image_url") == null || scene.get("audioArr") == null) {
                    throw new RuntimeException("씬 " + i + "의 데이터가 유효하지 않음");
                }
                
                // 임시 파일 경로 생성
                String tempSceneDir = TEMP_DIR + "/scene_" + i + "_" + UUID.randomUUID();
                
                File sceneDir = new File(tempSceneDir);
                if (!sceneDir.exists()) {
                    sceneDir.mkdirs();
                }

                // 오디오 URL 목록 수집
                List<String> audioUrls = new ArrayList<>();
                List<Map<String, Object>> audioArr = (List<Map<String, Object>>) scene.get("audioArr");

                for (Map<String, Object> audio : audioArr) {
                    String audioUrl = (String) audio.get("audio_url");
                    if (audioUrl == null) {
                        throw new RuntimeException("씬 " + i + "에 오디오 URL이 없음");
                    }
                    audioUrls.add(audioUrl);
                }

                // 오디오 파일 병합
                String mergedAudioPath = tempSceneDir + "/merged_audio.mp3";
                mergeAudioFiles(audioUrls, mergedAudioPath);

                // 이미지와 병합된 오디오로 비디오 생성
                String sceneVideoPath = tempSceneDir + "/scene_video.mp4";
                createVideoFromImageAndAudio((String) scene.get("image_url"), mergedAudioPath, sceneVideoPath);

                sceneVideoPaths.add(sceneVideoPath);

                // 중간 파일 삭제
                new File(mergedAudioPath).delete();
            }
            
            // 모든 씬 비디오 병합하여 최종 비디오 생성
            File finalVideo = mergeVideos(sceneVideoPaths, cleanOutputPath);
            
            // 임시 씬 비디오 파일들 삭제
            for (String path : sceneVideoPaths) {
                new File(path).delete();
                // 부모 디렉토리도 삭제
                new File(new File(path).getParent()).delete();
            }

            return finalVideo;

        } catch (Exception e) {
            logger.error("최종 비디오 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("최종 비디오 생성 실패: " + e.getMessage(), e);
        }
    }

    public String createAndUploadVideo(String storyId, String outputPath) {
        try {
            // UUID를 이용한 임시 파일 경로 생성
            String tempOutputPath = TEMP_DIR + "/upload_" + UUID.randomUUID() + ".mp4";
            String cleanOutputPath = tempOutputPath.replace("\"", "");
            logger.info("비디오 생성 및 업로드 시작", storyId);

            // 비디오 생성
            File videoFile = createFinalVideo(storyId, cleanOutputPath);
            
            // S3에 업로드할 키 생성
            String timestamp = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd_HHmmss")
                .format(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")));
            
            // storyId 패딩 적용 (8자리로 맞추기)
            String paddedStoryId = String.format("%08d", Integer.parseInt(storyId));
            String s3Key = paddedStoryId + "/videos/" + paddedStoryId + "_" + timestamp + ".mp4";
            
            // S3에 업로드
            logger.info("S3 업로드: {}", s3Key);
            s3Config.uploadToS3(videoFile.getPath(), s3Key);
            
            // 임시 파일 삭제
            videoFile.delete();
            
            // S3 url 반환
            String s3Url = "https://" + s3Config.getBucketName() + ".s3." + s3Config.getRegion() + ".amazonaws.com/" + s3Key;

            return s3Url;
        } catch (Exception e) {
            logger.error("비디오 생성 및 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("비디오 생성 및 업로드 중 오류 발생", e);
        }
    }

    // 비디오 엔티티 초기화
    public void initVideoEntity(String storyId) {
        Story story = storyRepository.findById(Long.parseLong(storyId))
                .orElseThrow(() -> new RuntimeException("스토리를 찾을 수 없음: " + storyId));
        
        Video video = new Video();
        video.setStory(story);
        video.setStatus(VideoStatus.PENDING);
        video.setCreatedAt(LocalDateTime.now());
        
        videoRepository.save(video);
    }
    
    // 비디오 상태 업데이트
    public void updateVideoStatus(String storyId, VideoStatus status, String message) {
        Video video = videoRepository.findByStoryId(Long.parseLong(storyId))
                .orElseThrow(() -> new RuntimeException("비디오 엔티티를 찾을 수 없음: " + storyId));
        
        video.setStatus(status);
        
        if (status == VideoStatus.COMPLETED) {
            video.setVideo_url(message);
            video.setCompletedAt(LocalDateTime.now());
        } else if (status == VideoStatus.FAILED) {
            video.setErrorMessage(message);
        }
        
        videoRepository.save(video);
    }
    
    // 비디오 상태 조회
    public VideoStatusResponseDto getVideoStatus(String storyId) {
        Video video = videoRepository.findByStoryId(Long.parseLong(storyId))
                .orElseThrow(() -> new RuntimeException("비디오 엔티티를 찾을 수 없음: " + storyId));
        
        VideoStatusResponseDto dto = new VideoStatusResponseDto();
        dto.setStoryId(storyId);
        dto.setStatus(video.getStatus());
        
        // 날짜 변환
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dto.setCreatedAt(video.getCreatedAt() != null ? video.getCreatedAt().format(formatter) : null);
        
        // status에 따라 필요한 필드만 설정
        if (video.getStatus() == VideoStatus.COMPLETED) {
            dto.setVideoUrl(video.getVideo_url());
            dto.setCompletedAt(video.getCompletedAt() != null ? video.getCompletedAt().format(formatter) : null);
        } else if (video.getStatus() == VideoStatus.FAILED) {
            dto.setErrorMessage(video.getErrorMessage());
        }
        
        return dto;
    }

    /**
     * title :          story.title
     * status :         video.status
     * completedAt :    생성 완료 시간 : video.completedAt
     * sumnail_url :    썸네일(00:00) -> 첫번째 이미지 보여주자! => 첫 번째 scene의 image URL을 pre-signed url 로 변경해서 반환
     * videoUrl:        video URL을 Presigned URL로 변경해서 반환
     * storyId :        story.story_id
     */
    public VideoListResponseDTO getAllVideoStatus() {
        // 모든 비디오 엔티티 가져옴
        List<Video> videos = videoRepository.findAll();

        // 메소드 결과를 담을 리스트
        List<VideoStatusAllDTO> result = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 각 비디오에 대해 반복
        for (Video video : videos) {
            // Video 연관된 story 정보 가져옴
            Story story = video.getStory();

            String title = story.getTitle();
            String storyId = String.valueOf(story.getId());
            String completedAt = video.getCompletedAt() != null
                    ? video.getCompletedAt().format(formatter)
                    : null;

            //썸네일용 Presigned Url 생성
            String thumbnailUrl = getFirstImageURL(storyId);
            
            // video_url이 있는 경우에만 presigned URL 생성
            String videoUrl = null;
            if (video.getVideo_url() != null) {
                String videoS3Key = s3Config.extractS3KeyFromUrl(video.getVideo_url());
                videoUrl = s3Config.generatePresignedUrl(videoS3Key);
            }

            VideoStatusAllDTO dto = new VideoStatusAllDTO(
                    title,
                    video.getStatus(),
                    completedAt,
                    thumbnailUrl,
                    videoUrl,
                    storyId
            );

            //결과 list에 추가
            result.add(dto);
        }
        return new VideoListResponseDTO(result);
    }

    //이미지 presigned URL 생성 메소드
    private String getFirstImageURL(String storyId) {
        // 스토리 id로 MongoDB에서 ScenceDocument 조회
        Optional<SceneDocument> sceneOpt = sceneDocumentRepository.findByStoryId(storyId);
        log.info("sceneDocument : {}",sceneOpt);

        if (sceneOpt.isEmpty()) {
            log.warn("해당 story에 해당하는 SceneDocument가 없음 {}", storyId);
            return null;
        }

        // SceneDocument 꺼내기
        SceneDocument sceneDocument = sceneOpt.get();
        System.out.println("scenedoc : "+sceneDocument);

        // SceneArr 가져오기.
        List<Map<String, Object>> sceneArr = sceneDocument.getSceneArr();

        if (sceneArr == null || sceneArr.isEmpty()) {
            log.warn("sceneDocument에는 sceneArr가 비어있음. {}",storyId);
            return null;
        }

        // 썸네일용 첫번째 scene 가져오기 + url 꺼내기
        Map<String, Object> firstScene = sceneArr.getFirst();
        String imageUrl = (String) firstScene.get("image_url");

        //Presigned URL 생성
        String S3Key = s3Config.extractS3KeyFromUrl(imageUrl);
        String PresignedUrl = s3Config.generatePresignedUrl(S3Key);

        return PresignedUrl;

    }
}