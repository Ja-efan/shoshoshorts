# 🚀 쇼쇼숓 (ShowShowShoot)

## 📌 프로젝트 개요
사용자가 입력한 스토리를 기반으로 다양한 AI를 활용하여 숏폼 콘텐츠를 제작하는 웹 플랫폼입니다.

## 🛠 기술 스택

### 프론트엔드
- React 18.3 (Vite 기반)
- TypeScript
- TailwindCSS
- Redux (상태 관리)
- Node.js 22.12.0
- npm 10.9.0

### 백엔드
- Java 21
- Spring Boot 3.2.3
- Spring Data JPA
- PostgreSQL 16
- Gradle 8.5

### 인프라
- Docker & Docker Compose
- Jenkins (CI/CD)

## 📂 프로젝트 구조
```
📦 프로젝트 루트
├── 📂 FE                   # 프론트엔드 애플리케이션
│   ├── 📂 src              # 소스 코드
│   └── 📜 Dockerfile       # 프론트엔드 도커 이미지 설정
│
├── 📂 BE                   # 백엔드 애플리케이션
│   ├── 📂 src              # 소스 코드
│   └── 📜 Dockerfile              # 백엔드 도커 이미지 설정
│
├── 📂 AI                   # AI 관련 코드
│
├── 📂 jenkins              # Jenkins CI/CD 설정
│   └── 📜 docker-compose.yml # Jenkins 컨테이너 설정
│
├── 📜 docker-compose.yml     # 배포용 컴포즈 파일
├── 📜 docker-compose.dev.yml # 개발용 컴포즈 파일
├── 📜 .env.example           # 환경 변수 예제 파일
├── 📜 Jenkinsfile            # CI/CD 파이프라인 설정
├── 📜 run-backend.sh         # 백엔드 실행 스크립트
├── 📜 run-frontend.sh        # 프론트엔드 실행 스크립트
├── 📜 run-backend-dev.sh     # 백엔드 개발 환경 실행 스크립트
├── 📜 run-frontend-dev.sh    # 프론트엔드 개발 환경 실행 스크립트
└── 📜 stop-containers.sh     # 컨테이너 중지 및 삭제 스크립트
```

## 🚀 설치 및 실행 방법

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
스크립트를 실행하기 전에 실행 권한을 부여해야 합니다:
```bash
chmod +x run-backend.sh run-frontend.sh run-backend-dev.sh run-frontend-dev.sh stop-containers.sh
```

### 개발 환경 실행
```bash
# 백엔드 개발 서비스 실행 (데이터베이스 포함)
./run-backend-dev.sh

# 프론트엔드 개발 서비스 실행
./run-frontend-dev.sh

# 또는 모든 서비스 한 번에 실행
docker-compose -f docker-compose.dev.yml up -d
```

### 배포 환경 실행
```bash
# 백엔드 서비스 실행 (데이터베이스 포함)
./run-backend.sh

# 프론트엔드 서비스 실행
./run-frontend.sh

# 또는 모든 서비스 한 번에 실행
docker-compose up -d
```

### 컨테이너 중지 및 삭제
```bash
# 모든 컨테이너 중지 및 삭제
./stop-containers.sh all

# 개발 환경 컨테이너만 중지 및 삭제
./stop-containers.sh dev

# 배포 환경 컨테이너만 중지 및 삭제
./stop-containers.sh prod
```

## 📝 접속 정보 (로컬 환경 기준)
- **백엔드 API**: http://localhost:8080
- **프론트엔드 (배포)**: http://localhost:80
- **프론트엔드 (개발)**: http://localhost:3000
- **데이터베이스**: localhost:5432
- **Jenkins**: http://localhost:8090

## 🔄 개발 워크플로우

1. 개발 환경 컨테이너 실행
2. 로컬에서 코드 수정 (BE 또는 FE 디렉토리)
3. 변경 사항 확인:
   - 백엔드: `http://localhost:8080`
   - 프론트엔드: `http://localhost:3000` (서버 초기화에 약 10초 소요)
4. 개발 완료 후 컨테이너 중지
5. 커밋 후 푸시 

## ⚠️ 주의사항

- 동일한 포트를 사용하는 서비스가 이미 실행 중인 경우 포트 충돌이 발생할 수 있습니다.
- 개발 환경과 배포 환경을 동시에 실행할 경우 포트 충돌이 발생할 수 있으므로 주의하세요.
- 데이터베이스 데이터는 Docker 볼륨에 저장되므로, 볼륨을 삭제하면 데이터가 손실됩니다.
- 프론트엔드 개발 서버는 초기화에 약 10초 정도 소요됩니다. 스크립트 실행 후 잠시 기다려주세요. (너무 오래 걸리는 경우 컨테이너 로그 확인으로 진행 상황 확인 가능)

## 📋 TODO
- 음성 변환 기능 구현
- 백엔드 API 연동
- UI 디자인 및 스타일링 개선

## 👥 팀원 및 역할
- 프론트엔드 개발: 김다현, 임종훈
- 백엔드 개발: 김근휘, 유정현, 제서윤
- AI 모델 개발: 연재환, 임종훈
- 인프라: 연재환

---
© 2024 쇼쇼숓 팀. 모든 권리 보유. 