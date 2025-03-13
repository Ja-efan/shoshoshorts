# 🚀 쇼쇼숓 - Docker 설정

## 📌 프로젝트 개요
사용자가 입력한 스토리를 기반으로 다양한 AI를 활용하여 숏폼 콘텐츠를 제작하는 웹 플랫폼을 개발합니다.

## 🐳 Docker 설정

### 사전 요구사항
- Docker 및 Docker Compose 설치
- Git

### 환경 설정
1. 환경 변수 파일 생성
```bash
cp .env.example .env
```
2. `.env` 파일을 열고 필요한 환경 변수 값을 설정합니다.

### 개발 환경 실행
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 배포 환경 실행
```bash
docker-compose up -d
```

### 개별 서비스 실행 스크립트
프로젝트에는 각 서비스를 독립적으로 실행할 수 있는 스크립트가 포함되어 있습니다.

#### 배포 환경 스크립트
```bash
# 백엔드 서비스 실행 (데이터베이스 포함)
./run-backend.sh

# 프론트엔드 서비스 실행
./run-frontend.sh
```

#### 개발 환경 스크립트
```bash
# 백엔드 개발 서비스 실행 (데이터베이스 포함)
./run-backend-dev.sh

# 프론트엔드 개발 서비스 실행
./run-frontend-dev.sh
```

#### 컨테이너 중지 및 삭제
```bash
# 모든 컨테이너 중지 및 삭제
./stop-containers.sh all

# 개발 환경 컨테이너만 중지 및 삭제
./stop-containers.sh dev

# 배포 환경 컨테이너만 중지 및 삭제
./stop-containers.sh prod

# 특정 서비스 중지 및 삭제 (예: 백엔드)
./stop-containers.sh backend

# 개발 환경의 특정 서비스 중지 및 삭제 (예: 프론트엔드)
./stop-containers.sh frontend dev
```

### Jenkins CI/CD 설정
1. Jenkins 컨테이너 실행
```bash
cd jenkins
docker-compose up -d
```
2. Jenkins 초기 설정
   - 브라우저에서 `http://localhost:8090` 접속
   - 초기 관리자 비밀번호 확인: `docker exec sss-jenkins cat /var/jenkins_home/secrets/initialAdminPassword`
   - 권장 플러그인 설치 및 관리자 계정 생성
3. Jenkins 파이프라인 설정
   - 새 파이프라인 작업 생성
   - SCM에서 Git 선택 및 저장소 URL 입력
   - Jenkinsfile 경로 지정: `Jenkinsfile`

## 🛠️ 도커 컨테이너 구성
- **Backend**: Java 21, Gradle 8.5
- **Frontend**: Node.js 22.12.0, npm 10.9.0
- **Database**: PostgreSQL 16
- **CI/CD**: Jenkins LTS

## 📂 도커 파일 구조
```
📦 프로젝트 루트
├── 📂 BE
│   ├── 📄 Dockerfile        # 백엔드 도커 이미지 설정
├── 📂 FE
│   ├── 📄 Dockerfile        # 프론트엔드 도커 이미지 설정
├── 📂 jenkins
│   ├── 📄 docker-compose.yml # Jenkins 컨테이너 설정
├── 📄 docker-compose.yml    # 배포용 컴포즈 파일
├── 📄 docker-compose.dev.yml # 개발용 컴포즈 파일
├── 📄 .env.example          # 환경 변수 예제 파일
├── 📄 Jenkinsfile           # CI/CD 파이프라인 설정
├── 📄 run-backend.sh        # 백엔드 실행 스크립트
├── 📄 run-frontend.sh       # 프론트엔드 실행 스크립트
├── 📄 run-backend-dev.sh    # 백엔드 개발 환경 실행 스크립트
├── 📄 run-frontend-dev.sh   # 프론트엔드 개발 환경 실행 스크립트
├── 📄 stop-containers.sh    # 컨테이너 중지 및 삭제 스크립트
└── 📄 README.md             # 프로젝트 설명서
``` 

## 📝 포트 정보
- **백엔드 API**: http://localhost:8080
- **프론트엔드 (배포)**: http://localhost:80
- **프론트엔드 (개발)**: http://localhost:3000 (Vite 개발 서버 5173 포트를 3000으로 매핑)
- **데이터베이스**: localhost:5432
- **Jenkins**: http://localhost:8090 