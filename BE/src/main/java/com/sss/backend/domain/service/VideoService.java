package com.sss.backend.domain.service;

import com.sss.backend.config.S3Config;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);
    
    // Docker 내부 경로 직접 지정
    private static final String DOCKER_TEMP_DIR = "/app/temp/videos";
    
    @Value("${temp.directory}")
    private String tempDirectory;
    
    private final SceneDocumentRepository sceneDocumentRepository;
    private final S3Config s3Config;
    private final FFmpeg ffmpeg;
    private final StoryRepository storyRepository;
    private final VideoRepository videoRepository;
    
    @PostConstruct
    public void init() {
        logger.info("TEMP_DIRECTORY 환경변수: {}", tempDirectory);
        // Docker 임시 디렉토리 생성
        createDockerTempDir();
    }
    
    // Docker 임시 디렉토리 생성
    private void createDockerTempDir() {
        File dir = new File(DOCKER_TEMP_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            logger.info("Docker 임시 디렉토리 생성: {} (성공: {})", DOCKER_TEMP_DIR, created);
            if (!created) {
                logger.error("Docker 임시 디렉토리 생성 실패. 권한 또는 경로 문제가 있을 수 있습니다: {}", DOCKER_TEMP_DIR);
                // 상위 디렉토리 생성 시도
                File parentDir = new File("/app/temp");
                if (!parentDir.exists()) {
                    boolean parentCreated = parentDir.mkdirs();
                    logger.info("상위 디렉토리 생성: {} (성공: {})", parentDir.getAbsolutePath(), parentCreated);
                    
                    // 다시 시도
                    created = dir.mkdirs();
                    logger.info("Docker 임시 디렉토리 재시도 생성: {} (성공: {})", DOCKER_TEMP_DIR, created);
                }
            }
        } else {
            logger.info("Docker 임시 디렉토리가 이미 존재합니다: {}", DOCKER_TEMP_DIR);
        }
        
        // 디렉토리 쓰기 권한 확인
        if (dir.exists() && dir.isDirectory()) {
            boolean canWrite = dir.canWrite();
            logger.info("Docker 임시 디렉토리 쓰기 권한: {}", canWrite);
            if (!canWrite) {
                logger.error("Docker 임시 디렉토리에 쓰기 권한이 없습니다: {}", DOCKER_TEMP_DIR);
            }
        }
    }
    
    public File mergeAudioFiles(List<String> audioUrls, String outputPath) {
        try {
            if (audioUrls.isEmpty()) {
                throw new RuntimeException("병합할 오디오 URL이 없습니다");
            }
            
            // 항상 먼저 임시 디렉토리 생성 확인
            createDockerTempDir();
            
            // Docker 볼륨 내부 경로로 직접 지정
            String dockerOutputPath = DOCKER_TEMP_DIR + "/merged_" + UUID.randomUUID() + ".mp3";
            logger.info("Docker 볼륨 내부 오디오 출력 경로: {}", dockerOutputPath);
            
            // 출력 디렉토리 생성
            File outputDir = new File(new File(dockerOutputPath).getParent());
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                logger.info("출력 디렉토리 생성: {} (성공: {})", outputDir.getAbsolutePath(), created);
            }
            
            // 디렉토리가 존재하는지 한번 더 확인
            if (!outputDir.exists() || !outputDir.isDirectory()) {
                logger.error("출력 디렉토리가 여전히 존재하지 않습니다: {}", outputDir.getAbsolutePath());
                
                // 직접 명령어로 디렉토리 생성 시도
                try {
                    Process process = Runtime.getRuntime().exec("mkdir -p " + DOCKER_TEMP_DIR);
                    int exitCode = process.waitFor();
                    logger.info("직접 명령으로 디렉토리 생성 시도. 종료 코드: {}", exitCode);
                } catch (Exception e) {
                    logger.error("직접 명령으로 디렉토리 생성 실패: {}", e.getMessage());
                }
            }
            
            // 첫 번째 오디오를 출력 파일로 복사
            String firstS3Key = extractS3KeyFromUrl(audioUrls.get(0));
            String firstPresignedUrl = s3Config.generatePresignedUrl(firstS3Key);
            logger.info("첫 번째 오디오 URL: {}", firstPresignedUrl);
            
            // 따옴표를 제거하고 경로 문자열을 직접 전달
            String cleanDockerOutputPath = dockerOutputPath.replace("\"", "");
            
            // 첫 번째 파일 처리
            FFmpegBuilder firstBuilder = new FFmpegBuilder()
                .setInput(firstPresignedUrl)
                .addExtraArgs("-y") // 파일이 이미 존재하면 덮어쓰기
                .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls") // HTTPS 프로토콜 허용
                .addOutput(cleanDockerOutputPath) // 따옴표 없이 경로 전달
                .setAudioCodec("libmp3lame")
                .setFormat("mp3")
                .done();
                
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(firstBuilder).run();
            
            // 파일이 생성됐는지 확인
            File outputFile = new File(cleanDockerOutputPath);
            logger.info("첫 번째 오디오 파일 생성 확인: {} (존재함: {})", cleanDockerOutputPath, outputFile.exists());
            
            // 추가 오디오 파일이 있으면 하나씩 병합
            for (int i = 1; i < audioUrls.size(); i++) {
                String tempOutput = DOCKER_TEMP_DIR + "/temp_" + UUID.randomUUID() + ".mp3";
                String cleanTempOutput = tempOutput.replace("\"", "");
                
                String s3Key = extractS3KeyFromUrl(audioUrls.get(i));
                String presignedUrl = s3Config.generatePresignedUrl(s3Key);
                logger.info("추가 오디오 URL ({}): {}", i, presignedUrl);
                
                // 현재 출력 파일과 다음 오디오 파일을 병합
                FFmpegBuilder appendBuilder = new FFmpegBuilder()
                    .addInput(cleanDockerOutputPath)
                    .addInput(presignedUrl)
                    .addExtraArgs("-y") // 파일이 이미 존재하면 덮어쓰기
                    .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls") // HTTPS 프로토콜 허용
                    .addOutput(cleanTempOutput) // 따옴표 없이 경로 전달
                    .addExtraArgs("-filter_complex", "[0:a][1:a]concat=n=2:v=0:a=1[outa]")
                    .addExtraArgs("-map", "[outa]")
                    .setAudioCodec("libmp3lame")
                    .setFormat("mp3")
                    .done();
                    
                executor.createJob(appendBuilder).run();
                
                // 임시 파일을 출력 파일로 이동
                Files.move(Paths.get(cleanTempOutput), Paths.get(cleanDockerOutputPath), StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 원래 요청된 출력 경로에 복사
            Files.copy(Paths.get(cleanDockerOutputPath), Paths.get(outputPath), StandardCopyOption.REPLACE_EXISTING);
            
            return new File(outputPath);
        } catch (Exception e) {
            logger.error("오디오 파일 병합 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("오디오 파일 병합 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    public File createVideoFromImageAndAudio(String imageUrl, String audioPath, String outputPath) {
        try {
            // 항상 먼저 임시 디렉토리 생성 확인
            createDockerTempDir();
            
            // Docker 볼륨 내부 경로로 직접 지정
            String dockerOutputPath = DOCKER_TEMP_DIR + "/video_" + UUID.randomUUID() + ".mp4";
            String dockerAudioPath = DOCKER_TEMP_DIR + "/audio_" + UUID.randomUUID() + ".mp3";
            
            // 따옴표 제거
            String cleanDockerOutputPath = dockerOutputPath.replace("\"", "");
            String cleanDockerAudioPath = dockerAudioPath.replace("\"", "");
            
            logger.info("Docker 볼륨 내부 비디오 출력 경로: {}", cleanDockerOutputPath);
            
            // 오디오 파일 복사
            Files.copy(Paths.get(audioPath), Paths.get(cleanDockerAudioPath), StandardCopyOption.REPLACE_EXISTING);
            
            // 출력 디렉토리 생성
            File outputDir = new File(new File(cleanDockerOutputPath).getParent());
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                logger.info("출력 디렉토리 생성: {} (성공: {})", outputDir.getAbsolutePath(), created);
            }
            
            // S3 pre-signed URL 생성
            String imageS3Key = extractS3KeyFromUrl(imageUrl);
            String presignedImageUrl = s3Config.generatePresignedUrl(imageS3Key);
            logger.info("이미지 URL: {}", presignedImageUrl);

            // FFmpeg 명령 구성 (이미지 URL 직접 사용)
            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(presignedImageUrl)  // pre-signed URL 직접 사용
                .addInput(cleanDockerAudioPath)
                .addExtraArgs("-y") // 파일이 이미 존재하면 덮어쓰기
                .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls") // HTTPS 프로토콜 허용
                .addOutput(cleanDockerOutputPath) // 따옴표 없이 경로 전달
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
            
            // 원래 요청된 출력 경로에 복사
            Files.copy(Paths.get(cleanDockerOutputPath), Paths.get(outputPath), StandardCopyOption.REPLACE_EXISTING);
            
            return new File(outputPath);
        } catch (Exception e) {
            logger.error("이미지와 오디오 합성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("이미지와 오디오 합성 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    public File mergeVideos(List<String> videoPaths, String outputPath) {
        try {
            // 항상 먼저 임시 디렉토리 생성 확인
            createDockerTempDir();
            
            // Docker 볼륨 내부 경로로 직접 지정
            String dockerOutputPath = DOCKER_TEMP_DIR + "/merged_video_" + UUID.randomUUID() + ".mp4";
            String cleanDockerOutputPath = dockerOutputPath.replace("\"", "");
            logger.info("Docker 볼륨 내부 병합 비디오 출력 경로: {}", cleanDockerOutputPath);
            
            // 출력 디렉토리 생성
            File outputDir = new File(new File(cleanDockerOutputPath).getParent());
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                logger.info("출력 디렉토리 생성: {} (성공: {})", outputDir.getAbsolutePath(), created);
            }
            
            // 비디오 파일을 Docker 볼륨으로 복사
            List<String> dockerVideoPaths = new ArrayList<>();
            for (int i = 0; i < videoPaths.size(); i++) {
                String dockerVideoPath = DOCKER_TEMP_DIR + "/scene_video_" + i + "_" + UUID.randomUUID() + ".mp4";
                String cleanDockerVideoPath = dockerVideoPath.replace("\"", "");
                Files.copy(Paths.get(videoPaths.get(i)), Paths.get(cleanDockerVideoPath), StandardCopyOption.REPLACE_EXISTING);
                dockerVideoPaths.add(cleanDockerVideoPath);
                logger.info("비디오 파일 복사: {} -> {}", videoPaths.get(i), cleanDockerVideoPath);
            }
            
            // 임시 파일 생성 (파일 목록)
            Path listFilePath = Paths.get(DOCKER_TEMP_DIR, "video_list_" + UUID.randomUUID() + ".txt");
            logger.info("비디오 목록 파일 경로: {}", listFilePath);
            
            StringBuilder fileList = new StringBuilder();
            for (String videoPath : dockerVideoPaths) {
                fileList.append("file '").append(videoPath).append("'\n");
                logger.info("비디오 목록에 추가: {}", videoPath);
            }
            
            // 목록 파일 디렉토리 생성
            File listDir = new File(listFilePath.getParent().toString());
            if (!listDir.exists()) {
                boolean created = listDir.mkdirs();
                logger.info("목록 파일 디렉토리 생성: {} (성공: {})", listDir.getAbsolutePath(), created);
            }
            
            Files.write(listFilePath, fileList.toString().getBytes());
            
            // FFmpeg 명령 구성
            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(listFilePath.toString())
                .addExtraArgs("-y") // 파일이 이미 존재하면 덮어쓰기
                .addExtraArgs("-f", "concat")  // concat 형식 명시
                .addExtraArgs("-safe", "0")    // 안전하지 않은 파일 경로 허용
                .addOutput(cleanDockerOutputPath) // 따옴표 없이 경로 전달
                .setVideoCodec("libx264")
                .setAudioCodec("aac")
                .setFormat("mp4")
                .done();
                
            // 실행
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();
            
            // 임시 파일 삭제
            Files.delete(listFilePath);
            
            // 원래 요청된 출력 경로에 복사
            Files.copy(Paths.get(cleanDockerOutputPath), Paths.get(outputPath), StandardCopyOption.REPLACE_EXISTING);
            
            return new File(outputPath);
        } catch (IOException e) {
            logger.error("비디오 병합 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("비디오 병합 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    public File createFinalVideo(String storyId, String outputPath) {
        try {
            // Docker 볼륨 내부 경로로 직접 지정
            String dockerOutputPath = DOCKER_TEMP_DIR + "/final_" + UUID.randomUUID() + ".mp4";
            logger.info("스토리 ID {} 에 대한 비디오 생성 시작 (Docker 출력 경로: {})", storyId, dockerOutputPath);
            
            // 1. 스토리 조회 및 유효성 검사
            Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
            if (sceneDocumentOpt.isEmpty()) {
                logger.error("스토리를 찾을 수 없음: {}", storyId);
                throw new RuntimeException("스토리를 찾을 수 없음: " + storyId);
            }
            
            SceneDocument sceneDocument = sceneDocumentOpt.get();
            List<Map<String, Object>> scenes = sceneDocument.getSceneArr();
            
            if (scenes == null || scenes.isEmpty()) {
                logger.error("스토리에 씬이 없음: {}", storyId);
                throw new RuntimeException("스토리에 씬이 없음: " + storyId);
            }

            List<String> sceneVideoPaths = new ArrayList<>();
            
            // 2. 각 Scene별로 처리
            for (int i = 0; i < scenes.size(); i++) {
                Map<String, Object> scene = scenes.get(i);
                logger.info("씬 처리 중 {}/{}", i + 1, scenes.size());
                
                // 씬 데이터 유효성 검사
                if (scene.get("image_url") == null || scene.get("audioArr") == null) {
                    logger.error("씬 {}의 데이터가 유효하지 않음", i);
                    throw new RuntimeException("씬 " + i + "의 데이터가 유효하지 않음");
                }
                
                // 임시 파일 경로 생성 - Docker 볼륨 내부 경로로 직접 지정
                String tempSceneDir = DOCKER_TEMP_DIR + "/scene_" + i + "_" + UUID.randomUUID();
                logger.info("씬 임시 디렉토리 (Docker 볼륨): {}", tempSceneDir);
                
                File sceneDir = new File(tempSceneDir);
                if (!sceneDir.exists()) {
                    boolean created = sceneDir.mkdirs();
                    logger.info("씬 디렉토리 생성: {} (성공: {})", sceneDir.getAbsolutePath(), created);
                }
                
                try {
                    // 오디오 URL 목록 수집
                    List<String> audioUrls = new ArrayList<>();
                    List<Map<String, Object>> audioArr = (List<Map<String, Object>>) scene.get("audioArr");
                    
                    for (int j = 0; j < audioArr.size(); j++) {
                        String audioUrl = (String) audioArr.get(j).get("audio_url");
                        logger.info("오디오 URL 처리 중: {}", audioUrl);
                        
                        if (audioUrl == null) {
                            throw new RuntimeException("Missing audio URL in scene " + i + ", audio " + j);
                        }
                        
                        audioUrls.add(audioUrl);
                    }
                    
                    // 오디오 파일 병합 - Docker 볼륨 내부 경로로 직접 지정
                    String mergedAudioPath = tempSceneDir + "/merged_audio.mp3";
                    mergeAudioFiles(audioUrls, mergedAudioPath);
                    logger.info("오디오 파일 병합 완료: {}", mergedAudioPath);
                    
                    // 이미지와 병합된 오디오로 비디오 생성
                    String sceneVideoPath = tempSceneDir + "/scene_video.mp4";
                    createVideoFromImageAndAudio((String) scene.get("image_url"), mergedAudioPath, sceneVideoPath);
                    logger.info("씬 비디오 생성 완료: {}", sceneVideoPath);
                    
                    sceneVideoPaths.add(sceneVideoPath);
                    
                } catch (Exception e) {
                    logger.error("씬 {} 처리 중 오류 발생: {}", i, e.getMessage(), e);
                    throw new RuntimeException("Failed to process scene " + i, e);
                }
            }
            
            // 3. 모든 씬 비디오 병합하여 최종 비디오 생성
            File finalVideo = mergeVideos(sceneVideoPaths, dockerOutputPath);
            logger.info("최종 비디오 생성 완료: {}", dockerOutputPath);
            
            // 원래 요청된 출력 경로에 복사
            Files.copy(finalVideo.toPath(), Paths.get(outputPath), StandardCopyOption.REPLACE_EXISTING);
            logger.info("최종 비디오 복사 완료: {} -> {}", dockerOutputPath, outputPath);
            
            return new File(outputPath);
            
        } catch (Exception e) {
            logger.error("최종 비디오 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("최종 비디오 생성 실패: " + e.getMessage(), e);
        }
    }

    public String createAndUploadVideo(String storyId, String outputPath) {
        try {
            // Docker 볼륨 내부 경로로 직접 지정
            String dockerOutputPath = DOCKER_TEMP_DIR + "/upload_" + UUID.randomUUID() + ".mp4";
            logger.info("비디오 생성 및 업로드 시작 (Docker 출력 경로: {})", dockerOutputPath);
            
            // 비디오 생성
            File videoFile = createFinalVideo(storyId, dockerOutputPath);
            
            // S3에 업로드할 키 생성
            // 날짜 형식의 타임스탬프 생성 (YYYYMMDD_HHMMSS) - 한국 시간(KST) 적용
            String timestamp = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd_HHmmss")
                .format(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")));
            
            // storyId 패딩 적용 (8자리로 맞추기)
            String paddedStoryId = String.format("%08d", Integer.parseInt(storyId));
            
            String s3Key = paddedStoryId + "/videos/" + paddedStoryId + "_" + timestamp + ".mp4";
            
            // S3에 업로드
            logger.info("S3 업로드 시작: {} -> {}", dockerOutputPath, s3Key);
            s3Config.uploadToS3(videoFile.getPath(), s3Key);
            logger.info("S3 업로드 완료: {}", s3Key);
            
            // 임시 파일 삭제
            videoFile.delete();
            
             // S3 url 반환
            String s3Url = "https://" + s3Config.getBucketName() + ".s3." + s3Config.getRegion() + ".amazonaws.com/" + s3Key;
            logger.info("S3 URL 생성: {}", s3Url);
            
            return s3Url;
        } catch (Exception e) {
            logger.error("비디오 생성 및 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("비디오 생성 및 업로드 중 오류 발생", e);
        }
    }

    // URL에서 S3 키를 추출하는 헬퍼 메소드 추가
    private String extractS3KeyFromUrl(String url) {
        // URL 형식: https://shoshoshorts.s3.ap-northeast-2.amazonaws.com/project1/audios/file.mp3
        String[] parts = url.split(".amazonaws.com/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("잘못된 S3 URL 형식: " + url);
        }
        return parts[1]; // project1/audios/file.mp3 부분만 반환
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
            video.setVideo_url(message); // 완료 시 URL 저장
            video.setCompletedAt(LocalDateTime.now());
        } else if (status == VideoStatus.FAILED) {
            video.setErrorMessage(message); // 실패 시 에러 메시지 저장
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
} 