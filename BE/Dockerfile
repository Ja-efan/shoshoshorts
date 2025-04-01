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

# ffmpeg 및 한글 폰트 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ffmpeg \
    fontconfig \
    wget \
    && rm -rf /var/lib/apt/lists/*

# 나눔 고딕 폰트만 직접 다운로드 및 설치
RUN mkdir -p /usr/share/fonts/truetype/nanum && \
    wget "https://github.com/google/fonts/raw/main/ofl/nanumgothic/NanumGothic-Regular.ttf" -O /usr/share/fonts/truetype/nanum/NanumGothic-Regular.ttf && \
    wget "https://github.com/google/fonts/raw/main/ofl/nanumgothic/NanumGothic-Bold.ttf" -O /usr/share/fonts/truetype/nanum/NanumGothic-Bold.ttf && \
    chmod 644 /usr/share/fonts/truetype/nanum/*.ttf

# 폰트 캐시 갱신 및 확인
RUN fc-cache -fv && fc-list | grep -i nanum

WORKDIR /app

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