# ✅ BE 코드 정적 분석 구성 정리 (Jenkins + GitLab + SonarQube)

## 📌 프로젝트 환경
- **백엔드 구조**: `BE/` 폴더에 Spring Boot + Gradle 프로젝트 구성
- **Git 저장소**: GitLab (`Dev/web` 브랜치 기준)
- **환경파일 관리**: `.env` 파일은 Git에 포함하지 않고, Jenkins Credentials로 관리됨

---

## 🛠️ Jenkins 구성

### 1. Freestyle Job 생성 및 설정
- GitLab 저장소 클론 (https://lab.ssafy.com/s12-ai-speech-sub1/S12P21B106.git)
- GitLab Webhook 연결 (Push 이벤트 기반)
- **JDK 21 자동 설치** 설정 (Adoptium `jdk-21.0.6+7`)

### 2. `.env` 파일 처리
- `.env` 파일을 Jenkins Credentials에 **Secret file**로 등록
<img src="PROJECT_IMAGES\sonarqube-environment.png">
- Jenkins Build Step - Shell에서 복사

```bash
cp "$ENV_FILE" BE/.env
```

### 3. Gradle 빌드
- 컴파일된 `.class` 파일을 생성하기 위한 빌드 실행
- 테스트는 생략하여 빠르게 분석 가능

```bash
cd BE
./gradlew clean build -x test
cd ..
```

---

## 🔎 SonarQube 설정

### Analysis properties 설정 예시

```properties
sonar.projectKey=S12P21B106
sonar.projectName=S12P21B106
sonar.sources=BE/src/main/java
sonar.java.binaries=BE/build/classes/java/main
sonar.host.url=https://sonarqube.ssafy.com
sonar.login=SonarQube-Token
```

### Build Step 순서

1. `.env` 파일 복사 및 Gradle 빌드  
<img src="PROJECT_IMAGES\sonarqube-build-steps-01.png">
3. SonarQube 분석 수행
<img src="PROJECT_IMAGES\sonarqube-build-steps-02.png">

---

## ⚙️ SonarQube 분석 동작 원리

1. Jenkins에서 SonarScanner 실행
2. 지정된 `sonar.*` 설정을 기반으로 프로젝트 분석 시작
3. `.java` 및 `.class` 파일을 활용해 코드 품질 분석
4. 분석 결과를 SonarQube 서버로 전송
5. SonarQube 대시보드에서 시각적으로 확인 가능

---

## 📊 분석 결과 확인

- [SonarQube 대시보드 바로가기](https://sonarqube.ssafy.com/dashboard?id=S12P21B106)

---

## 🎯 구성 요약

| 구성 요소 | 설명 |
|-----------|------|
| GitLab | 코드 저장소 및 웹훅 트리거 |
| Jenkins | 자동화 빌드 및 분석 파이프라인 |
| SonarQube | 정적 분석 및 품질 시각화 |
| Secret `.env` | 민감 정보 보안 관리 |
| JDK 21 | 프로젝트에 맞는 빌드 환경 |

---

## ✅ 향후 확장 가능 항목

- [ ] **FE 코드 정적 분석 추가 (예: ESLint, SonarScanner for JS)**
- [ ] **테스트 커버리지 연동 (JaCoCo, Kover 등)**
- [ ] **PR/MR 기반 분석 설정**
- [ ] **Quality Gate 실패 시 빌드 중단 설정**