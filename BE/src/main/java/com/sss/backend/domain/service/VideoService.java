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
import org.springframework.core.io.ClassPathResource;
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
import com.sss.backend.domain.entity.Users;
import com.sss.backend.domain.repository.UserRepository;
import com.sss.backend.domain.entity.VideoProcessingStep;
import com.sss.backend.domain.service.VideoProcessingStatusService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "sss_app_temp";
    private static final String BACKGROUND_IMAGE_PATH = "images/background.png";
    private static final String BACKGROUND_MUSIC_PATH = "audios"; // 배경음악 폴더 경로
    private String backgroundImageFilePath;
    private List<String> backgroundMusicFilePaths = new ArrayList<>();
    
    private final SceneDocumentRepository sceneDocumentRepository;
    private final S3Config s3Config;
    private final FFmpeg ffmpeg;
    private final StoryRepository storyRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoProcessingStatusService videoProcessingStatusService;

    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    @PostConstruct
    public void init() {
        createTempDir();
        copyBackgroundImage();
        copyBackgroundMusic();
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

        // Create subdirectories
        String[] subdirs = {"audios", "images", "videos", "subtitles"};
        for (String subdir : subdirs) {
            File subdirFile = new File(TEMP_DIR + File.separator + subdir);
            if (!subdirFile.exists()) {
                boolean created = subdirFile.mkdirs();
                logger.info("서브 디렉토리 생성: {} (성공: {})", subdirFile.getPath(), created);
            }
        }
    }

    /**
     * 클래스패스에서 배경 이미지를 복사하여 임시 디렉토리에 저장하는 메소드
     */
    private void copyBackgroundImage() {
        try {
            // 리소스 경로에서 이미지 파일 로드
            ClassPathResource resource = new ClassPathResource(BACKGROUND_IMAGE_PATH);
            if (!resource.exists()) {
                logger.error("배경 이미지 파일을 찾을 수 없음: {}", BACKGROUND_IMAGE_PATH);
                return;
            }

            // 임시 디렉토리에 복사
            String destPath = TEMP_DIR + File.separator + "images" + File.separator + "background.png";
            File destFile = new File(destPath);

            // 파일 복사
            Files.copy(resource.getInputStream(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            backgroundImageFilePath = destFile.getAbsolutePath();
            logger.info("배경 이미지 파일 복사 완료: {}", backgroundImageFilePath);
        } catch (IOException e) {
            logger.error("배경 이미지 파일 복사 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 클래스패스에서 배경 음악 파일들을 복사하여 임시 디렉토리에 저장하는 메소드
     */
    private void copyBackgroundMusic() {
        try {
            // ResourcePatternResolver를 사용하여 모든 mp3 파일을 찾음
            Resource[] resources = resourcePatternResolver.getResources("classpath:" + BACKGROUND_MUSIC_PATH + "/*.mp3");
            
            if (resources.length == 0) {
                logger.warn("배경 음악 파일이 없음: {}", BACKGROUND_MUSIC_PATH);
                return;
            }

            // 임시 디렉토리에 복사
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                String destPath = TEMP_DIR + File.separator + "audios" + File.separator + "bg_" + filename;
                File destFile = new File(destPath);
                
                // 파일 복사 (InputStream 사용)
                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    backgroundMusicFilePaths.add(destFile.getAbsolutePath());
                    logger.info("배경 음악 파일 복사 완료: {}", destFile.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            logger.error("배경 음악 파일 복사 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 가용한 배경 음악 파일 중 하나를 랜덤하게 선택
     * @return 선택된 배경 음악 파일 경로, 없으면 null
     */
    private String getRandomBackgroundMusic() {
        if (backgroundMusicFilePaths.isEmpty()) {
            logger.warn("사용 가능한 배경 음악 파일이 없습니다.");
            return null;
        }
        
        int randomIndex = (int) (Math.random() * backgroundMusicFilePaths.size());
        return backgroundMusicFilePaths.get(randomIndex);
    }

    public File mergeAudioFiles(List<String> audioUrls, String outputPath) {
        try {
            if (audioUrls.isEmpty()) {
                throw new RuntimeException("병합할 오디오 URL이 없습니다");
            }
            
            createTempDir();

            // 임시 출력 경로는 중간 작업에만 사용
            String tempOutputPath = TEMP_DIR + File.separator + "audios" + File.separator + "merged_" + UUID.randomUUID() + ".mp3";
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
                String tempOutput = TEMP_DIR + File.separator + "audios" + File.separator + "temp_" + UUID.randomUUID() + ".mp3";
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

            // 배경 이미지 파일 확인
            if (backgroundImageFilePath == null || !new File(backgroundImageFilePath).exists()) {
                logger.warn("배경 이미지 파일이 없어서 다시 복사를 시도합니다.");
                copyBackgroundImage();

                if (backgroundImageFilePath == null) {
                    logger.error("배경 이미지 파일을 찾을 수 없어 기본 배경으로 대체합니다.");
                }
            }

            String cleanOutputPath = outputPath.replace("\"", "");
            String tempAudioPath = TEMP_DIR + File.separator + "audios" + File.separator + UUID.randomUUID() + ".mp3";
            String cleanAudioPath = tempAudioPath.replace("\"", "");

            // 오디오 파일 복사 (필수 과정)
            Files.copy(Paths.get(audioPath), Paths.get(cleanAudioPath), StandardCopyOption.REPLACE_EXISTING);

            // S3 pre-signed URL 생성
            String imageS3Key = s3Config.extractS3KeyFromUrl(imageUrl);
            String presignedImageUrl = s3Config.generatePresignedUrl(imageS3Key);
            logger.info("이미지 URL 생성: {}", presignedImageUrl);

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);

            if (backgroundImageFilePath != null && new File(backgroundImageFilePath).exists()) {
                logger.info("배경 이미지 사용: {}", backgroundImageFilePath);

                // 배경 이미지와 메인 이미지를 한 번에 처리
                // 1. 배경 이미지 입력
                // 2. S3 이미지 URL 입력
                // 3. filter_complex로 이미지 리사이즈 및 오버레이
                FFmpegBuilder builder = new FFmpegBuilder()
                    .addInput(backgroundImageFilePath)
                    .addInput(presignedImageUrl)
                    .addInput(cleanAudioPath)
                    .addExtraArgs("-y")
                    .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls")
                    .addOutput(cleanOutputPath)
                    .addExtraArgs("-filter_complex",
                        "[1:v]scale=800:800[fg];" +    // 메인 이미지를 800x800으로 조정
                        "[0:v][fg]overlay=(540-w/2):(1250-h/2)[v];" +  // 이미지 중앙이 (540,1250)에 오도록 배치
                        "[v][2:a]concat=n=1:v=1:a=1[outv][outa]")  // 비디오와 오디오 결합
                    .addExtraArgs("-map", "[outv]")
                    .addExtraArgs("-map", "[outa]")
                    .setVideoCodec("libx264")
                    .setConstantRateFactor(23)
                    .setVideoPixelFormat("yuv420p")
                    .setAudioCodec("aac")
                    .setAudioBitRate(128000)
                    .setFormat("mp4")
                    .done();

                executor.createJob(builder).run();
                logger.info("비디오 생성 완료 (배경 이미지 적용): {}", cleanOutputPath);
            } else {
                logger.warn("배경 이미지를 찾을 수 없어 기본 배경으로 대체합니다.");

                // 배경 이미지 없이 처리
                FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(presignedImageUrl)
                    .addInput(cleanAudioPath)
                    .addExtraArgs("-y")
                    .addExtraArgs("-protocol_whitelist", "file,http,https,tcp,tls")
                    .addOutput(cleanOutputPath)
                    .addExtraArgs("-filter_complex",
                        "[0:v]scale=900:900,pad=1080:1920:90:510:white[v];" +  // 900x900으로 조정 및 흰색 패딩
                        "[v][1:a]concat=n=1:v=1:a=1[outv][outa]")
                    .addExtraArgs("-map", "[outv]")
                    .addExtraArgs("-map", "[outa]")
                    .setVideoCodec("libx264")
                    .setConstantRateFactor(23)
                    .setVideoPixelFormat("yuv420p")
                    .setAudioCodec("aac")
                    .setAudioBitRate(128000)
                    .setFormat("mp4")
                    .done();
                
                executor.createJob(builder).run();
                logger.info("비디오 생성 완료 (기본 배경): {}", cleanOutputPath);
            }
            
            // 임시 오디오 파일 삭제
            new File(cleanAudioPath).delete();

            return new File(cleanOutputPath);
        } catch (Exception e) {
            logger.error("이미지와 오디오 합성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("이미지와 오디오 합성 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    public File mergeVideos(List<String> videoPaths, String outputPath, String storyId) {
        try {
            createTempDir();

            String cleanOutputPath = outputPath.replace("\"", "");
            String tempOutputPath = TEMP_DIR + File.separator + "videos" + File.separator + "merged_without_subs_" + UUID.randomUUID() + ".mp4";
            String cleanTempOutputPath = tempOutputPath.replace("\"", "");
            String tempWithMusicPath = TEMP_DIR + File.separator + "videos" + File.separator + "merged_with_music_" + UUID.randomUUID() + ".mp4";
            String cleanTempWithMusicPath = tempWithMusicPath.replace("\"", "");

            // 비디오 파일을 임시 경로로 복사 (여러 파일 병합 위해 필요한 과정)
            List<String> tempVideoPaths = new ArrayList<>();
            for (int i = 0; i < videoPaths.size(); i++) {
                String tempVideoPath = TEMP_DIR + File.separator + "videos" + File.separator + "scene_video_" + i + "_" + UUID.randomUUID() + ".mp4";
                String cleanVideoPath = tempVideoPath.replace("\"", "");
                
                // 원본 파일 존재 확인
                File originalFile = new File(videoPaths.get(i));
                if (!originalFile.exists() || originalFile.length() == 0) {
                    logger.error("비디오 파일이 존재하지 않거나 크기가 0입니다: {}", videoPaths.get(i));
                    continue;  // 존재하지 않는 파일은 건너뜀
                }
                
                Files.copy(Paths.get(videoPaths.get(i)), Paths.get(cleanVideoPath), StandardCopyOption.REPLACE_EXISTING);
                tempVideoPaths.add(cleanVideoPath);
                logger.info("비디오 파일 복사 완료 {}/{}: {}", i+1, videoPaths.size(), cleanVideoPath);
            }
            
            // 파일이 하나도 없으면 실패 처리
            if (tempVideoPaths.isEmpty()) {
                throw new RuntimeException("병합할 유효한 비디오 파일이 없습니다");
            }

            // 임시 파일 목록 (나중에 삭제를 위해 추적)
            List<String> tempFilesToDelete = new ArrayList<>(tempVideoPaths);

            // 임시 파일 생성 (파일 목록)
            Path listFilePath = Paths.get(TEMP_DIR, "videos" + File.separator + "video_list_" + UUID.randomUUID() + ".txt");
            
            StringBuilder fileList = new StringBuilder();
            for (String videoPath : tempVideoPaths) {
                fileList.append("file '").append(videoPath.replace("\\", "\\\\").replace("'", "\\'")).append("'\n");
            }
            
            Files.write(listFilePath, fileList.toString().getBytes());
            logger.info("비디오 리스트 파일 생성: {} (총 {}개 비디오)", listFilePath, tempVideoPaths.size());
            tempFilesToDelete.add(listFilePath.toString());
            
            // 1단계: 먼저 비디오만 병합 (자막 없이)
            FFmpegBuilder mergeBuilder = new FFmpegBuilder()
                .setInput(listFilePath.toString())
                .addExtraArgs("-y")
                .addExtraArgs("-f", "concat")
                .addExtraArgs("-safe", "0")
                .addOutput(cleanTempOutputPath)
                .setVideoCodec("libx264")
                .setConstantRateFactor(23) // 품질 설정 (0-51, 낮을수록 고품질)
                .setVideoPixelFormat("yuv420p") // 유튜브 호환 픽셀 포맷
                .setAudioCodec("aac")
                .setAudioBitRate(128000) // 128kbps
                .setFormat("mp4")
                .done();
                
            // 실행
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(mergeBuilder).run();
            
            // 병합된 파일 확인
            File mergedFile = new File(cleanTempOutputPath);
            if (!mergedFile.exists() || mergedFile.length() == 0) {
                throw new RuntimeException("비디오 병합에 실패했습니다: 결과 파일이 존재하지 않거나 크기가 0입니다");
            }
            
            // 원본 병합 영상의 길이 확인을 위한 FFprobe 실행 코드 추가
            double videoDuration = 0;
            try {
                String[] ffprobeCmd = {
                    ffmpeg.getPath().replace("ffmpeg", "ffprobe"),
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    cleanTempOutputPath
                };
                Process process = Runtime.getRuntime().exec(ffprobeCmd);
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String durationStr = reader.readLine();
                videoDuration = Double.parseDouble(durationStr);
                logger.info("원본 병합 영상 길이: {}초", videoDuration);
            } catch (Exception e) {
                logger.warn("영상 길이 확인 중 오류: {}", e.getMessage());
            }
            
            // 현재 작업 중인 파일 경로
            String currentVideoPath = cleanTempOutputPath;
            tempFilesToDelete.add(currentVideoPath);
            
            // 2단계: 병합된 비디오에 자막 추가
            File subtitleFile = null;
            try {
                subtitleFile = createSubtitleFile(storyId);
                tempFilesToDelete.add(subtitleFile.getAbsolutePath());
                
                FFmpegBuilder subtitleBuilder = new FFmpegBuilder()
                    .setInput(currentVideoPath)
                    .addExtraArgs("-y")
                    .addOutput(cleanOutputPath)
                    .setVideoCodec("libx264")
                    .setConstantRateFactor(23) // 품질 설정
                    .setVideoPixelFormat("yuv420p") // 유튜브 호환 픽셀 포맷
                    .setAudioCodec("aac")
                    .setAudioBitRate(128000) // 128kbps
                    .addExtraArgs("-vf", "ass=" + subtitleFile.getAbsolutePath().replace("\\", "\\\\").replace(":", "\\:"))
                    .setFormat("mp4")
                    .done();
                    
                executor.createJob(subtitleBuilder).run();

                // 3단계: 배경 음악 추가
                String backgroundMusic = getRandomBackgroundMusic();
                if (backgroundMusic != null && new File(backgroundMusic).exists()) {
                    logger.info("배경 음악 추가: {}", backgroundMusic);
                    
                    // 영상 길이에 따라 처리 방법 분리
                    if (videoDuration < 3.0) {
                        logger.info("매우 짧은 영상({}초)에 대한 특수 처리 적용", videoDuration);
                        
                        // 짧은 영상에는 단순히 배경 음악만 낮은 볼륨으로 추가하고 원본 오디오 보존
                        FFmpegBuilder musicBuilder = new FFmpegBuilder()
                            .setInput(cleanOutputPath)
                            .addInput(backgroundMusic)
                            .addExtraArgs("-y")
                            .addOutput(cleanTempWithMusicPath)
                            .addExtraArgs("-filter_complex", 
                                "[1:a]volume=0.3,aloop=loop=-1:size=2e+09[a1];" + 
                                "[0:a][a1]amerge=inputs=2[aout]")  // amix 대신 amerge 사용
                            .addExtraArgs("-map", "0:v")
                            .addExtraArgs("-map", "[aout]")
                            .setVideoCodec("copy")
                            .setAudioCodec("aac")
                            .setAudioBitRate(192000)
                            .setFormat("mp4")
                            .done();
                            
                        executor.createJob(musicBuilder).run();
                    } else {
                        // 기존 방식대로 처리 (3초 이상 영상)
                        float dropoutTransition = videoDuration < 10 ? 0.5f : 3.0f;
                        logger.info("설정된 dropout_transition 값: {}", dropoutTransition);
                        
                        FFmpegBuilder musicBuilder = new FFmpegBuilder()
                            .setInput(cleanOutputPath)
                            .addInput(backgroundMusic)
                            .addExtraArgs("-y")
                            .addOutput(cleanTempWithMusicPath)
                            .addExtraArgs("-filter_complex", 
                                "[1:a]volume=0.3,aloop=loop=-1:size=2e+09[a1];" + 
                                "[0:a][a1]amix=inputs=2:duration=first:dropout_transition=" + dropoutTransition + "[aout]")
                            .addExtraArgs("-map", "0:v")
                            .addExtraArgs("-map", "[aout]")
                            .setVideoCodec("copy")
                            .setAudioCodec("aac")
                            .setAudioBitRate(192000)
                            .setFormat("mp4")
                            .done();
                            
                        executor.createJob(musicBuilder).run();
                    }
                    
                    // 배경음악이 추가된 파일 확인
                    File musicAddedFile = new File(cleanTempWithMusicPath);
                    if (musicAddedFile.exists() && musicAddedFile.length() > 0) {
                        // 배경 음악이 추가된 영상 길이 확인
                        try {
                            String[] ffprobeCmd = {
                                ffmpeg.getPath().replace("ffmpeg", "ffprobe"),
                                "-v", "error",
                                "-show_entries", "format=duration",
                                "-of", "default=noprint_wrappers=1:nokey=1",
                                cleanTempWithMusicPath
                            };
                            Process process = Runtime.getRuntime().exec(ffprobeCmd);
                            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                            String duration = reader.readLine();
                            logger.info("배경 음악 추가 후 영상 길이: {}초", duration);
                            
                            // 만약 결과 영상이 원본보다 짧다면 원본을 사용
                            double newDuration = Double.parseDouble(duration);
                            if (newDuration < videoDuration * 0.9) {  // 10% 이상 짧아졌다면
                                logger.warn("배경 음악 추가 후 영상이 짧아짐: {}초 -> {}초, 자막만 있는 영상 사용", videoDuration, newDuration);
                                // 자막만 있는 비디오 경로가 최종 결과
                            } else {
                                // 새 파일 문제없이 생성되었을 때는 최종 결과를 배경 음악이 있는 파일로 설정
                                // cleanOutputPath를 최종 결과로 유지하고, 배경 음악이 있는 파일을 복사
                                Files.copy(Paths.get(cleanTempWithMusicPath), Paths.get(cleanOutputPath), StandardCopyOption.REPLACE_EXISTING);
                                logger.info("배경 음악이 추가된 비디오를 최종 결과로 설정: {} (파일 크기: {}bytes)", cleanOutputPath, new File(cleanOutputPath).length());
                            }
                        } catch (Exception e) {
                            logger.warn("영상 길이 확인 중 오류: {}", e.getMessage());
                            // 오류 발생 시 자막만 있는 영상 사용 (이미 cleanOutputPath에 있음)
                        }
                    } else {
                        // 배경 음악 추가 실패 시 자막만 있는 비디오 유지
                        logger.warn("배경 음악 추가 실패, 자막만 있는 비디오를 유지합니다");
                    }
                } else {
                    logger.warn("배경 음악을 찾을 수 없어 생략합니다.");
                }
                
                // 최종 결과 확인
                File resultFile = new File(cleanOutputPath);
                if (!resultFile.exists() || resultFile.length() == 0) {
                    // 자막 추가나 배경 음악 추가가 모두 실패한 경우 원본 병합 비디오를 결과로 사용
                    logger.warn("최종 처리 실패, 원본 병합 비디오를 복사합니다");
                    Files.copy(Paths.get(cleanTempOutputPath), Paths.get(cleanOutputPath), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                logger.error("자막 처리 중 오류 발생: {}", e.getMessage());
                // 자막 실패 시 현재 비디오를 최종 결과로 사용
                try {
                    Files.copy(Paths.get(cleanTempOutputPath), Paths.get(cleanOutputPath), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException copyEx) {
                    logger.error("원본 비디오 복사 중 오류 발생: {}", copyEx.getMessage());
                }
            }
            
            // 최종 결과 파일 확인
            File finalOutput = new File(cleanOutputPath);
            if (!finalOutput.exists() || finalOutput.length() == 0) {
                throw new RuntimeException("최종 비디오 생성 실패: 파일이 존재하지 않거나 크기가 0입니다");
            }
            
            // 임시 파일 정리 (최종 결과 파일이 확인된 후에만)
            for (String tempFile : tempFilesToDelete) {
                try {
                    Files.deleteIfExists(Paths.get(tempFile));
                } catch (IOException e) {
                    logger.warn("임시 파일 삭제 실패 (무시됨): {}", tempFile);
                }
            }

            return finalOutput;
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
                String tempSceneDir = TEMP_DIR + File.separator + "videos" + File.separator + "scene_" + i + "_" + UUID.randomUUID();
                
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
                String mergedAudioPath = tempSceneDir + File.separator + "merged_audio.mp3";
                mergeAudioFiles(audioUrls, mergedAudioPath);

                // 이미지와 병합된 오디오로 비디오 생성
                String sceneVideoPath = tempSceneDir + File.separator + "scene_video.mp4";
                createVideoFromImageAndAudio((String) scene.get("image_url"), mergedAudioPath, sceneVideoPath);

                sceneVideoPaths.add(sceneVideoPath);

                // 중간 파일 삭제
                new File(mergedAudioPath).delete();
            }
            
            // 모든 씬 비디오 병합하여 최종 비디오 생성
            File finalVideo = mergeVideos(sceneVideoPaths, cleanOutputPath, storyId);
            
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

    /**
     * 비디오 상태를 완료로 업데이트하고 비디오 URL 설정
     */
    public void updateVideoCompleted(String storyId, String videoUrl) {
        // 상태 업데이트: 완료
        updateVideoStatus(storyId, VideoStatus.COMPLETED, videoUrl);

        // 처리 단계 정보 삭제 (완료되었으므로)
        videoProcessingStatusService.deleteProcessingStep(storyId);
    }

    /**
     * 비디오 상태를 실패로 업데이트하고 오류 메시지 설정
     */
    public void updateVideoFailed(String storyId, String errorMessage) {
        // 상태 업데이트: 실패
        updateVideoStatus(storyId, VideoStatus.FAILED, errorMessage);

        // 처리 단계 정보 삭제
        videoProcessingStatusService.deleteProcessingStep(storyId);
    }

    /**
     * 비디오 생성 및 S3 업로드 (상태 업데이트 포함)
     */
    public String createAndUploadVideo(String storyId, String outputPath) {
        try {
            // UUID를 이용한 임시 파일 경로 생성
            String tempOutputPath = TEMP_DIR + File.separator + "videos" + File.separator + "upload_" + UUID.randomUUID() + ".mp4";
            String cleanOutputPath = tempOutputPath.replace("\"", "");
            logger.info("비디오 생성 및 업로드 시작", storyId);

            // 비디오 렌더링 중 상태 업데이트
            videoProcessingStatusService.updateProcessingStep(storyId, VideoProcessingStep.VIDEO_RENDERING);

            // 비디오 생성
            File videoFile = createFinalVideo(storyId, cleanOutputPath);
            
            // 비디오 렌더링 완료 상태 업데이트
            videoProcessingStatusService.updateProcessingStep(storyId, VideoProcessingStep.VIDEO_RENDER_COMPLETED);

            // 비디오 업로드 중 상태 업데이트
            videoProcessingStatusService.updateProcessingStep(storyId, VideoProcessingStep.VIDEO_UPLOADING);

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
        } else if (video.getStatus() == VideoStatus.PROCESSING) {
            // 처리 단계 enum 값 자체를
            VideoProcessingStep step = videoProcessingStatusService.getProcessingStep(storyId);
            if (step != null) {
                dto.setProcessingStep(step.name()); // description 대신 name()을 사용
            }
        }
        
        return dto;
    }



    //이미지 presigned URL 생성 메소드
    public String getFirstImageURL(String storyId) {
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
        Map<String, Object> firstScene = sceneArr.get(0);
        String imageUrl = (String) firstScene.get("image_url");

        // image_url이 있는 경우에만 presigned URL 생성
        if (imageUrl != null) {
            String S3Key = s3Config.extractS3KeyFromUrl(imageUrl);
            return s3Config.generatePresignedUrl(S3Key);
        }

        log.warn("첫 번째 scene의 image_url이 null임. {}", storyId);
        return null;
    }

    private String extractStoryIdFromOutputPath(String outputPath) {
        // Extract storyId from the output path or use another method to get the storyId
        // This is a placeholder - implement based on your output path format
        String fileName = new File(outputPath).getName();
        if (fileName.contains("_")) {
            return fileName.substring(0, fileName.indexOf("_"));
        }
        return "";
    }

    private File createSubtitleFile(String storyId) throws IOException {
        // 스토리 문서 조회
        Optional<SceneDocument> sceneDocumentOpt = sceneDocumentRepository.findByStoryId(storyId);
        if (sceneDocumentOpt.isEmpty()) {
            throw new RuntimeException("스토리를 찾을 수 없음: " + storyId);
        }
        
        SceneDocument sceneDocument = sceneDocumentOpt.get();
        List<Map<String, Object>> scenes = sceneDocument.getSceneArr();
        
        // 기본 닉네임 설정
        String nickname = "사용자";
        
        try {
            // 스토리 ID로 Story 엔티티를 가져옴
            Story story = storyRepository.findById(Long.parseLong(storyId))
                    .orElseThrow(() -> new RuntimeException("스토리를 찾을 수 없음: " + storyId));
            
            // User 정보 직접 조회
            if (story.getUser() != null && story.getUser().getId() != null) {
                Long userId = story.getUser().getId();
                // UserRepository를 통해 직접 조회
                Optional<Users> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent() && userOpt.get().getNickname() != null) {
                    nickname = userOpt.get().getNickname();
                }
            }
        } catch (Exception e) {
            logger.warn("사용자 정보를 가져오는 중 오류 발생: {}", e.getMessage());
            // 기본값 "사용자"를 사용
        }
        
        // 현재 날짜 포맷팅 - 한국 시간대(KST) 사용
        // 사용자 시간대 설정을 추가하여 다양한 국가의 사용자들을 지원할 수 있도록 개선 필요...
        String currentDate = DateTimeFormatter.ofPattern("yy-MM-dd")
                .format(LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")));
        
        // ASS 형식의 자막 파일 생성
        File subtitleFile = new File(TEMP_DIR + File.separator + "subtitles" + File.separator + storyId + "_subtitles.ass");
        StringBuilder assContent = new StringBuilder();
        
        // ASS 헤더 추가
        assContent.append("[Script Info]\n");
        assContent.append("Title: ").append(sceneDocument.getStoryTitle()).append("\n");
        assContent.append("ScriptType: v4.00+\n");
        assContent.append("PlayResX: 1080\n");
        assContent.append("PlayResY: 1920\n");
        assContent.append("WrapStyle: 0\n");
        // assContent.append("ScaledBorderAndShadow: yes\n");  // 테두리와 그림자 스케일링 활성화
        assContent.append("\n");
        
        // 스타일 정의 수정
        assContent.append("[V4+ Styles]\n");
        assContent.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        assContent.append("Style: Default,NanumGothic,60,&H00000000,&H000000FF,&H00FFFFFF,&H88000000,1,0,0,0,100,100,0,0,1,3,2,5,100,100,0,1\n");
        assContent.append("Style: Title,NanumGothic,70,&H00000000,&H000000FF,&H00FFFFFF,&HCC000000,1,0,0,0,100,100,0,0,1,3,2,1,0,0,0,1\n");
        assContent.append("Style: UserInfo,NanumGothic,40,&H80808000,&H000000FF,&H00FFFFFF,&HCC000000,0,0,0,0,100,100,0,0,0,0,0,1,0,0,0,1\n");
        assContent.append("\n");
        
        // 자막 이벤트
        assContent.append("[Events]\n");
        assContent.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
        
        // 제목 추가 (영상 전체 시간동안 좌상단에 표시)
        String storyTitle = sceneDocument.getStoryTitle();
        assContent.append("Dialogue: 0,0:00:00.00,10:00:00.00,Title,,0,0,0,,{\\pos(70,365)}")
                .append(storyTitle)
                .append("\n");
        
        // 사용자 정보 추가 (닉네임과 생성 시간)
        assContent.append("Dialogue: 0,0:00:00.00,10:00:00.00,UserInfo,,0,0,0,,{\\pos(70,435)}")
                .append(nickname)
                .append(" • ")
                .append(currentDate)
                .append("\n");

        // 자막 시간 추적을 위한 변수 초기화
        double currentTime = 0.0;
         
        for (Map<String, Object> scene : scenes) {
            List<Map<String, Object>> audioArr = (List<Map<String, Object>>) scene.get("audioArr");
            
            for (Map<String, Object> audio : audioArr) {
                // 텍스트와 길이 가져오기
                String text = (String) audio.get("text");
                
                // 텍스트 길이에 따라 수동으로 줄바꿈 추가 (예: 20자 이상이면 중간에 줄바꿈)
                if (text.length() > 20) {
                    int midPoint = text.length() / 2;
                    // 공백 위치를 찾아 가장 가까운 위치에서 줄바꿈
                    int breakPoint = text.indexOf(" ", midPoint);
                    if (breakPoint == -1) breakPoint = midPoint; // 공백이 없으면 중간에서 자름
                    
                    text = text.substring(0, breakPoint) + "\\N" + text.substring(breakPoint).trim();
                }
                
                double duration = audio.containsKey("duration") ? 
                    ((Number) audio.get("duration")).doubleValue() : 3.0; // 기본값 3초
                
                // ASS 형식의 시간 문자열
                String startTime = formatAssTime(currentTime);
                String endTime = formatAssTime(currentTime + duration);
                
                // 자막 라인 추가 - 중앙이 (540,640)에 오도록 + 좌우 여백 추가
                assContent.append("Dialogue: 0,")
                         .append(startTime).append(",")
                         .append(endTime).append(",")
                         .append("Default,,100,100,0,,{\\pos(540,640)\\an5\\fs60}")
                         .append(text)
                         .append("\n");
                
                // 현재 시간 업데이트
                currentTime += duration;
            }
        }
        
        // 파일에 내용 쓰기
        Files.writeString(subtitleFile.toPath(), assContent.toString());
        return subtitleFile;
    }

    // ASS 형식의 시간 문자열로 변환
    private String formatAssTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int centiseconds = (int) ((seconds - Math.floor(seconds)) * 100);
        
        return String.format("%d:%02d:%02d.%02d", hours, minutes, secs, centiseconds);
    }

    /**
     * 배경 이미지가 올바르게 초기화되었는지 확인하는 테스트 메소드
     * @return 배경 이미지 파일 경로 (존재하지 않는 경우 null)
     */
    public String getBackgroundImagePath() {
        if (backgroundImageFilePath == null || !new File(backgroundImageFilePath).exists()) {
            logger.warn("배경 이미지 경로가 초기화되지 않았거나 파일이 없어서 다시 복사를 시도합니다.");
            copyBackgroundImage();
        }
        return backgroundImageFilePath;
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


        // 각 비디오에 대해 반복
        for (Video video : videos) {
            VideoStatusAllDTO dto = mapToVideoStatusDTO(video);
            //결과 list에 추가
            result.add(dto);
        }
        return new VideoListResponseDTO(result);
    }

    public VideoListResponseDTO getAllVideoStatusById(Long userId) {
        // 유저 아이디로 검색
        List<Video> videos = videoRepository.findByStory_User_Id(userId);
        List<VideoStatusAllDTO> result = new ArrayList<>();

        // 각 비디오에 대해 반복
        for (Video video : videos) {
            VideoStatusAllDTO dto = mapToVideoStatusDTO(video);
            //결과 list에 추가
            result.add(dto);
        }
        return new VideoListResponseDTO(result);

    }

    private VideoStatusAllDTO mapToVideoStatusDTO(Video video) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        // PROCESSING 상태인 경우에만 세부 처리 단계 정보 추가
        String processingStep = null;
        if (video.getStatus() == VideoStatus.PROCESSING) {
            VideoProcessingStep step = videoProcessingStatusService.getProcessingStep(storyId);
            if (step != null) {
                processingStep = step.name();
            }
        }

        return new VideoStatusAllDTO(
                title,
                video.getStatus(),
                completedAt,
                thumbnailUrl,
                videoUrl,
                storyId,
                processingStep
        );
    }

    public boolean deleteVideo(Long userId, Long videoId) {
        // 1. 비디오 ID로 비디오 조회
        Optional<Video> videoOpt = videoRepository.findById(videoId);

        if (videoOpt.isEmpty()) {
            return false; // 비디오가 존재하지 않음
        }

        Video video = videoOpt.get();

        // 2. 비디오의 소유자 확인 (Story의 User ID와 로그인한 사용자 ID 비교)
        if (!video.getStory().getUser().getId().equals(userId)) {
            return false; // 비디오 소유자가 아님
        }

        // 3. 비디오 삭제
        videoRepository.delete(video);

        // 4. S3 삭제(?)
        return true;
    }
}