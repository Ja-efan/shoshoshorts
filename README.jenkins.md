# Jenkins 파이프라인 및 CI/CD 설정 가이드

## 목차
1. [소개](#소개)
2. [Jenkins 컨테이너 설정](#jenkins-컨테이너-설정)
3. [Jenkins 설치 및 설정](#jenkins-설치-및-설정)
4. [파이프라인 구성](#파이프라인-구성)
5. [환경 변수 및 보안 설정](#환경-변수-및-보안-설정)
6. [프론트엔드 빌드 설정](#프론트엔드-빌드-설정)
7. [백엔드 빌드 설정](#백엔드-빌드-설정)
8. [배포 프로세스](#배포-프로세스)
9. [Docker-in-Docker 아키텍처](#docker-in-docker-아키텍처)
10. [문제 해결](#문제-해결)
11. [참고 자료](#참고-자료)

## 소개

이 문서는 Jenkins를 사용하여 SSS 프로젝트의 CI/CD 파이프라인을 설정하는 방법을 설명합니다. 
이 파이프라인은 코드 변경 사항을 감지하고, 자동으로 빌드하여 배포 서버에 배포하는 과정을 자동화합니다.

## Jenkins 컨테이너 설정

현재 프로젝트에서 Jenkins는 Docker 컨테이너로 실행되며, 다음과 같은 파일들로 구성되어 있습니다:

### docker-compose.yml

```yaml
version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:lts
    container_name: sss-jenkins
    privileged: true
    user: root
    ports:
      - "8090:8080"
      - "50000:50000"
    volumes:
      - jenkins-data:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
      - ./init.groovy:/var/jenkins_home/init.groovy.d/init.groovy
    networks:
      - jenkins-network
    restart: unless-stopped

networks:
  jenkins-network:
    driver: bridge

volumes:
  jenkins-data:
```

주요 설정 분석:
- **베이스 이미지**: `jenkins/jenkins:lts` - Jenkins의 LTS(Long Term Support) 버전 사용
- **컨테이너 이름**: `sss-jenkins`
- **권한 설정**: 
  - `privileged: true`: 컨테이너에 높은 권한 부여
  - `user: root`: root 사용자로 실행하여 Docker 명령어 실행 권한 확보
- **포트 매핑**: 
  - `8090:8080`: Jenkins 웹 인터페이스(호스트의 8090 → 컨테이너의 8080)
  - `50000:50000`: Jenkins Agent 연결용 포트
- **볼륨 마운트**:
  - `jenkins-data:/var/jenkins_home`: Jenkins 설정 및 데이터 영구 저장
  - `/var/run/docker.sock:/var/run/docker.sock`: 호스트의 Docker 소켓 마운트(Docker-in-Docker 방식)
  - `./init.groovy:/var/jenkins_home/init.groovy.d/init.groovy`: 초기화 스크립트
- **재시작 정책**: `restart: unless-stopped` - 명시적으로 중지하지 않는 한 항상 재시작

### init.groovy

```groovy
#!/usr/bin/env groovy

import jenkins.model.*
import hudson.model.*

println "Installing Docker CLI..."
def process = "apt-get update && apt-get install -y docker.io".execute()
process.waitFor()

println "Docker CLI installation completed with exit code: ${process.exitValue()}"
if (process.exitValue() != 0) {
    println "Error installing Docker CLI: ${process.err.text}"
} else {
    println "Docker CLI installed successfully"
}
```

이 스크립트는 Jenkins 컨테이너가 시작될 때 자동으로 실행되며, 다음 작업을 수행합니다:
- Jenkins 컨테이너 내부에 Docker CLI 설치
- Docker CLI가 없으면 패키지 매니저를 통해 설치
- 설치 결과 로깅

## Jenkins 설치 및 설정

### 사전 요구 사항
- Docker와 Docker Compose가 설치된 서버
- Git 저장소 접근 권한
- 도메인(선택 사항)

### Jenkins 설치

1. 프로젝트의 `jenkins` 디렉토리에 있는 `docker-compose.yml` 파일을 사용하여 Jenkins를 실행합니다:

```bash
cd jenkins
docker-compose up -d
```

2. Jenkins 초기 설정:
   - 브라우저에서 `http://서버IP:8090`에 접속
   - 초기 관리자 비밀번호는 다음 명령어로 확인할 수 있습니다:
   ```bash
   docker exec sss-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
   - 권장 플러그인 설치 및 관리자 계정 생성

### 필요한 플러그인 설치
- GitLab 
- GitLab API
- GitLab Branch Source
- Git Integration
- Docker Pipeline
- Pipeline
- Blue Ocean (선택 사항: 시각화된 파이프라인 UI)

## 파이프라인 구성

### Git 저장소 연결

1. Jenkins 관리 > Credentials > System > Global credentials > Add Credentials에서 Git 저장소 접근 자격 증명을 추가합니다.
   - Kind: Username with password, Gitlab API Token
   - ID: git-credentials (또는 원하는 ID)
   - 관련 정보 입력

2. 새 파이프라인 작업 생성:
   - Jenkins 홈 > New Item > Pipeline
   - 이름 입력: sss-pipeline (또는 원하는 이름)
   - Pipeline 스크립트 소스로 "Pipeline script from SCM" 선택
   - SCM: Git
   - Repository URL: 저장소 URL
   - Credentials: 이전에 생성한 자격 증명 선택
   - Branch Specifier: */release/web (배포용 브랜치)
   - Script Path: Jenkinsfile

### Jenkinsfile 구성

프로젝트 루트 디렉토리에 있는 `Jenkinsfile`은 다음과 같은 단계로 구성되어 있습니다:

```groovy
pipeline {
    agent any
    
    environment {
        DB_PASSWORD = credentials('db-password')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Backend') {
            steps {
                dir('BE') {
                    sh 'docker build -t sss-backend:latest .'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('FE') {
                    sh 'docker build -t sss-frontend:latest .'
                }
            }
        }
        
        stage('Deploy') {
            steps {
                sh 'docker compose down || true'
                sh 'docker compose up -d'
            }
        }
        
        stage('Cleanup') {
            steps {
                sh 'docker image prune -f'
            }
        }
    }
    
    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
```

파이프라인 단계 분석:
1. **환경 설정**: 데이터베이스 비밀번호와 같은 민감한 정보를 Jenkins Credentials에서 가져옴
2. **코드 체크아웃**: Git 저장소에서 최신 코드를 가져옴
3. **백엔드 빌드**: BE 디렉토리에서 Docker 이미지 빌드
4. **프론트엔드 빌드**: FE 디렉토리에서 Docker 이미지 빌드
5. **배포**: Docker Compose를 사용하여 서비스 배포
6. **정리**: 사용하지 않는 Docker 이미지 정리
7. **후처리**: 배포 성공 또는 실패 시 알림

## 환경 변수 및 보안 설정

### Jenkins Credentials 설정

1. Jenkins 관리 > Credentials > System > Global credentials > Add Credentials에서 다음 자격 증명을 추가합니다:
   - Kind: Secret text
   - Secret: 데이터베이스 비밀번호
   - ID: db-password

### .env 파일 설정 (선택 사항)

필요한 경우 Jenkins 작업 공간에 .env 파일을 생성하도록 파이프라인을 수정할 수 있습니다:

```groovy
stage('Prepare Environment') {
    steps {
        script {
            // .env 파일 생성
            sh '''
            cat > .env << EOF
            DB_PASSWORD=${DB_PASSWORD}
            MONGO_PASSWORD=${MONGO_PASSWORD}
            FRONTEND_PORT=80
            EOF
            '''
        }
    }
}
```

## 프론트엔드 빌드 설정

프론트엔드 빌드는 Nginx를 사용하여 정적 파일을 제공합니다:

1. `FE/Dockerfile`이 올바르게 설정되어 있는지 확인:
   - 빌드 단계에서 애플리케이션을 빌드
   - Nginx 이미지를 기반으로 한 실행 단계
   - Nginx 설정 파일 복사

2. `FE/nginx.conf` 파일이 프록시 설정을 포함하고 있는지 확인:
   - 백엔드 API 경로 설정
   - 정적 파일 제공 설정
   - SPA 라우팅 설정

## 백엔드 빌드 설정

백엔드 빌드 설정:

1. `BE/Dockerfile`이 올바르게 설정되어 있는지 확인:
   - 스프링 부트 애플리케이션 빌드
   - 환경 변수 구성

2. 데이터베이스 연결 설정이 정확한지 확인

## 배포 프로세스

배포 프로세스는 다음과 같이 진행됩니다:

1. Jenkins가 변경 사항을 감지하여 파이프라인 실행
2. 코드 체크아웃
3. 백엔드 및 프론트엔드 Docker 이미지 빌드
4. `docker-compose.yml`을 사용하여 서비스 배포:
   ```bash
   docker-compose down || true
   docker-compose up -d
   ```
5. 사용하지 않는 이미지 정리

### 수동 배포 (필요한 경우)

수동으로 배포해야 하는 경우:

1. 서버에 SSH로 접속
2. 프로젝트 디렉토리로 이동
3. 다음 명령어 실행:
   ```bash
   git pull
   docker-compose down
   docker-compose up -d
   ```

## Docker-in-Docker 아키텍처

현재 설정은 "Docker-in-Docker" 패턴을 활용하여 Jenkins가 호스트의 Docker 데몬을 사용하도록 구성되어 있습니다:

1. Jenkins 컨테이너는 호스트의 Docker 소켓(`/var/run/docker.sock`)을 마운트
2. Jenkins 컨테이너 내부에서 실행되는 작업은 호스트의 Docker 데몬을 사용하여 컨테이너 생성, 빌드, 실행 가능
3. 이 방식은 새로운 Docker 데몬을 실행하지 않고 호스트의 리소스를 효율적으로 사용

### CI/CD 워크플로우

전체 CI/CD 워크플로우는 다음과 같이 작동합니다:

1. 개발자가 Git 저장소의 지정된 브랜치(일반적으로 `release/web`)에 코드 변경사항을 푸시
2. Jenkins는 저장소의 변경을 감지하고 파이프라인 실행 시작
3. 파이프라인 단계에 따라 백엔드 및 프론트엔드 코드 빌드
4. Docker 이미지 생성 및 태그 지정
5. Docker Compose를 사용하여 새 버전의 서비스 배포
6. 사용하지 않는 Docker 이미지 정리

### 보안 및 권한

1. Jenkins는 root 사용자로 실행되어 Docker 명령어 실행 권한을 가짐
2. 민감한 정보는 Jenkins Credentials 시스템을 통해 관리
3. Docker 소켓에 대한 접근 권한이 필요하므로 호스트 사용자를 Docker 그룹에 추가

## 문제 해결

### 일반적인 문제

1. **이미지 빌드 실패**
   - 로그 확인: `docker logs sss-jenkins`
   - Dockerfile 구문 오류 확인
   - 빌드 환경의 리소스(메모리, CPU) 확인

2. **컨테이너 실행 실패**
   - 포트 충돌 확인: `netstat -tulpn | grep <포트번호>`
   - 환경 변수 설정 확인
   - Docker 볼륨 권한 확인

3. **Jenkins 권한 문제**
   - Docker 소켓 권한 확인: `ls -la /var/run/docker.sock`
   - Jenkins 컨테이너가 루트 권한으로 실행 중인지 확인
   - 호스트의 사용자가 Docker 그룹에 속해 있어야 함:
     `sudo usermod -aG docker username`
   - 그룹 변경 사항을 적용하려면 로그아웃 후 다시 로그인 또는 재부팅 필요

4. **포트 접근 문제**: 
   - 방화벽 설정에서 Jenkins 포트(8090) 허용 필요:
     `sudo ufw allow 8090/tcp`

5. **Jenkins 초기화 문제**: 
   - 로그 확인: `docker logs sss-jenkins`
   - 초기 비밀번호 확인: 
     `docker exec sss-jenkins cat /var/jenkins_home/secrets/initialAdminPassword`

## 참고 자료

- [Jenkins 공식 문서](https://www.jenkins.io/doc/)
- [Docker 공식 문서](https://docs.docker.com/)
- [Nginx 설정 가이드](https://nginx.org/en/docs/)
- [Docker Compose 문서](https://docs.docker.com/compose/)
