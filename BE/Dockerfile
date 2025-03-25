# 빌드 스테이지
FROM sss-backend-base:latest AS build
WORKDIR /app

# 그래들 파일 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드
RUN gradle dependencies --no-daemon

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN gradle build --no-daemon -x test

# 실행 스테이지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# ffmpeg 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 환경변수 설정 추가
ENV AWS_ACCESS_KEY=${AWS_ACCESS_KEY}
ENV AWS_SECRET_KEY=${AWS_SECRET_KEY}

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"] 