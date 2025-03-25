package com.sss.backend.config;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Configuration
@Getter
public class S3Config {

    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

    @Value("${aws.accessKey}")
    private String accessKey;
    
    @Value("${aws.secretKey}")
    private String secretKey;
    
    @Value("${aws.region:ap-northeast-2}")
    private String region;
    
    @Value("${aws.s3.bucket:shoshoshorts}")
    private String bucketName;
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
    public S3Config(
        @Value("${aws.accessKey}") String accessKey,
        @Value("${aws.secretKey}") String secretKey,
        @Value("${aws.region:ap-northeast-2}") String region
    ) {
        if (accessKey == null || secretKey == null) {
            throw new IllegalStateException(
                "AWS credentials not found. Please check your environment variables or application properties."
            );
        }
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        );
        
        Region awsRegion = Region.of(region);
        
        // S3 클라이언트 초기화
        this.s3Client = S3Client.builder()
            .region(awsRegion)
            .credentialsProvider(credentialsProvider)
            .build();
            
        // S3 Presigner 초기화
        this.s3Presigner = S3Presigner.builder()
            .region(awsRegion)
            .credentialsProvider(credentialsProvider)
            .build();
    }
    
    /**
     * S3에서 파일을 다운로드
     */
    public void downloadFromS3(String s3Key, String localPath) throws IOException {
        try {
            logger.debug("S3에서 파일 다운로드 시도: {}", s3Key);
            
            Path localFilePath = Paths.get(localPath);
            Files.createDirectories(localFilePath.getParent());
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
                
            Files.copy(
                s3Client.getObject(getObjectRequest),
                localFilePath,
                StandardCopyOption.REPLACE_EXISTING
            );
            
            logger.debug("파일 다운로드 성공: {}", s3Key);
        } catch (NoSuchKeyException e) {
            logger.error("S3에서 파일을 찾을 수 없음: {}", s3Key);
            throw new IOException("S3에서 파일을 찾을 수 없음: " + s3Key, e);
        } catch (S3Exception e) {
            logger.error("S3 접근 중 오류 발생: {}", e.getMessage());
            throw new IOException("S3 접근 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 파일을 S3에 업로드
     */
    public void uploadToS3(String localPath, String s3Key) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build();
            
        s3Client.putObject(request, Paths.get(localPath));
    }

    /**
     * URL에서 S3 키를 추출하는 헬퍼 메소드 추가
     */
    public String extractS3KeyFromUrl(String url) {
        // URL 형식: https://shoshoshorts.s3.ap-northeast-2.amazonaws.com/project1/audios/file.mp3
        String[] parts = url.split(".amazonaws.com/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("잘못된 S3 URL 형식: " + url);
        }
        return parts[1]; // project1/audios/file.mp3 부분만 반환
    }

    /**
     * S3 객체에 대한 pre-signed URL 생성
     */
    public String generatePresignedUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * 다운로드용 pre-signed URL (헤더에 ContentDisposition 포함)
     */
    public String generateDownloadPresignedUrl(String s3Key, String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(getObjectRequest)
            .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}