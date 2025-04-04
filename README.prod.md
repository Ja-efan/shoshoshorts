# README.prod.md

# 🚀 쇼쇼숏 - 배포 환경 설정 가이드

## 📌 프로젝트 개요
사용자가 입력한 스토리를 기반으로 다양한 AI를 활용하여 숏폼 콘텐츠를 제작하는 웹 플랫폼입니다.

## 🐳 배포 환경 설정

### 사전 요구사항
- Docker 및 Docker Compose 설치
- Git

### 환경 설정
1. 프로젝트 클론
```bash
git clone https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git
cd S12P21B106
```

2. 환경 변수 파일 생성
```bash
cp .env.example .env
```

3. `.env` 파일을 열고 필요한 환경 변수 값을 설정합니다.

### 실행 권한 부여
스크립트를 실행하기 전에 실행 권한을 부여해야 합니다.
```bash
chmod +x build-images.sh run-backend.sh run-frontend.sh stop-containers.sh
```

## 🏗 배포 환경 설정 순서

### 1. Docker 이미지 빌드
```bash
# 대화형 모드로 실행
./build-images.sh

# 또는 배포에 필요한 이미지만 빌드
./build-images.sh base prod
```

### 2. Docker Compose를 사용한 전체 서비스 실행
```bash
docker compose up -d
```

### 3. 개별 서비스 실행 스크립트 사용 (선택사항)
```bash
# 백엔드 서비스 실행 (데이터베이스 포함)
./run-backend.sh

# 프론트엔드 서비스 실행
./run-frontend.sh
```

### 4. 서비스 중지 및 삭제
```bash
# 배포 환경 컨테이너만 중지 및 삭제
./stop-containers.sh prod

# 특정 서비스 중지 및 삭제 (예: 백엔드)
./stop-containers.sh backend

# 특정 서비스 중지 및 삭제 (예: 프론트엔드)
./stop-containers.sh frontend
```

## 📡 Jenkins CI/CD 설정

1. Jenkins 컨테이너 실행
```bash
cd jenkins
docker compose up -d
```

2. Jenkins 초기 설정
   - 브라우저에서 `http://localhost:8090` 접속
   - 초기 관리자 비밀번호 확인: `docker exec sss-jenkins cat /var/jenkins_home/secrets/initialAdminPassword`
   - 권장 플러그인 설치 및 관리자 계정 생성

3. Jenkins 파이프라인 설정
   - 새 파이프라인 작업 생성
   - SCM에서 Git 선택 및 저장소 URL 입력
   - Jenkinsfile 경로 지정: `Jenkinsfile`

## 📝 포트 정보
- 백엔드 API: http://localhost:8080
- 프론트엔드: http://localhost:80
- PostgreSQL: localhost:5432
- MongoDB: localhost:27017
- Jenkins: http://localhost:8090

## ✅ 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 목록 확인
docker ps

# 컨테이너 로그 확인
docker logs sss-backend
docker logs sss-frontend
docker logs sss-postgres
docker logs sss-mongodb
docker logs sss-jenkins
```

## 🔄 시스템 의존성 관리

새로운 시스템 의존성 추가 방법:

1. 기본 Dockerfile 수정:
```bash
# 백엔드 의존성 추가
vi BE/Dockerfile.base
# 프론트엔드 의존성 추가
vi FE/Dockerfile.base
```

2. 이미지 재빌드:
```bash
./build-images.sh base prod
```

3. 컨테이너 재시작:
```bash
docker compose down
docker compose up -d
```

## ⚠️ 주의사항

- 동일한 포트를 사용하는 서비스가 이미 실행 중인 경우 포트 충돌이 발생할 수 있습니다.
- 개발 환경과 배포 환경을 동시에 실행할 경우 포트 충돌이 발생할 수 있으므로 주의하세요.
- 데이터베이스 데이터는 Docker 볼륨에 저장되므로, 볼륨을 삭제하면 데이터가 손실됩니다.