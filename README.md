# �� 쇼쇼숏(ShoShoShorts)

1. [프로젝트 개요](#-프로젝트-개요)
2. [기술 스택](#-기술-스택)
   - [프론트엔드](#프론트엔드)
   - [백엔드](#백엔드)
   - [인프라](#인프라)
3. [프로젝트 구조](#-프로젝트-구조)
4. [관련 문서](#-관련-문서)
5. [TODO](#-todo)
6. [팀원 및 역할](#-팀원-및-역할)

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
- Spring Boot 3.4.0
- Spring Data JPA
- Spring Data MongoDB
- PostgreSQL 16
- MongoDB 7.0
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

## 📚 관련 문서
- [Docker 설정 가이드](README.docker.md): Docker 환경 설정 및 실행 방법에 대한 상세 가이드
- [Shell 스크립트 가이드](README.shell.md): 프로젝트에서 사용되는 shell 스크립트들에 대한 설명

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
© 2025 쇼쇼숏 팀. 모든 권리 보유. 