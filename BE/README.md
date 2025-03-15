# SSS 백엔드 프로젝트

## 개요
이 프로젝트는 Spring Boot 기반의 RESTful API 서버입니다.

## 기술 스택
- Java 21
- Spring Boot 3.4.0
- Spring Data JPA
- Spring Data MongoDB
- PostgreSQL
- MongoDB 7.0
- Gradle 8.5

## 프로젝트 구조
```
BE/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sss/
│   │   │           └── backend/
│   │   │               ├── controller/    # REST API 컨트롤러
│   │   │               ├── model/         # 엔티티 모델
│   │   │               ├── repository/    # 데이터 액세스 계층
│   │   │               ├── service/       # 비즈니스 로직
│   │   │               └── BackendApplication.java  # 메인 애플리케이션 클래스
│   │   └── resources/
│   │       ├── application.properties     # 기본 설정
│   │       ├── application-dev.properties # 개발 환경 설정
│   │       └── application-prod.properties # 배포 환경 설정
│   └── test/  # 테스트 코드
├── build.gradle  # 빌드 설정
├── settings.gradle  # 프로젝트 설정
├── gradlew  # Gradle Wrapper 스크립트
└── Dockerfile  # Docker 이미지 빌드 설정
```

## API 엔드포인트

### 기본 API
- `GET /api/hello`: 기본 인사 메시지 반환

### 메시지 API
- `GET /api/messages`: 모든 메시지 조회
- `GET /api/messages/{id}`: ID로 메시지 조회
- `POST /api/messages`: 새 메시지 생성
- `PUT /api/messages/{id}`: 메시지 업데이트
- `DELETE /api/messages/{id}`: 메시지 삭제

## 로컬 개발 환경 설정

### 필수 요구사항
- Java 21
- PostgreSQL

### 애플리케이션 실행
```bash
# Gradle을 사용하여 애플리케이션 실행
./gradlew bootRun

# 개발 프로필로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Docker 환경에서 실행
```bash
# 개발 환경
../run-backend-dev.sh

# 배포 환경
../run-backend.sh
```

## 테스트 API 호출 예시
```bash
# Hello API 호출
curl http://localhost:8080/api/hello

# 메시지 생성
curl -X POST -H "Content-Type: application/json" -d '{"content":"안녕하세요!"}' http://localhost:8080/api/messages

# 모든 메시지 조회
curl http://localhost:8080/api/messages
```
