#!/bin/bash

# 환경 변수 파일 로드
if [ -f .env ]; then
  export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
else
  echo "환경 변수 파일(.env)이 없습니다. .env.example을 복사하여 사용하세요."
  echo "cp .env.example .env"
  exit 1
fi

# 네트워크가 없으면 생성
if ! docker network inspect sss-network &>/dev/null; then
  echo "Docker 네트워크 'sss-network'를 생성합니다..."
  docker network create sss-network
fi

# PostgreSQL 데이터베이스 컨테이너 실행
echo "개발용 PostgreSQL 데이터베이스 컨테이너를 실행합니다..."
docker run -d \
  --name sss-postgres-dev \
  -p 5432:5432 \
  -e POSTGRES_DB=bta_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -v postgres-data-dev:/var/lib/postgresql/data \
  --network sss-network \
  postgres:16-alpine

# MongoDB 데이터베이스 컨테이너 실행
echo "개발용 MongoDB 데이터베이스 컨테이너를 실행합니다..."
docker run -d \
  --name sss-mongo-dev \
  -p 27017:27017 \
  -e MONGO_INITDB_DATABASE=sss_db \
  -v mongo-data-dev:/data/db \
  --network sss-network \
  mongo:7.0

# 볼륨이 없으면 생성
if ! docker volume inspect gradle-cache &>/dev/null; then
  echo "Gradle 캐시 볼륨을 생성합니다..."
  docker volume create gradle-cache
fi

# 백엔드 개발 이미지가 없으면 빌드
if ! docker image inspect sss-backend-dev:latest &>/dev/null; then
  echo "백엔드 개발 이미지가 없습니다. 이미지를 빌드합니다..."
  ./build-images.sh base dev
fi

# 백엔드 개발 컨테이너 실행
echo "백엔드 개발 컨테이너를 실행합니다..."
docker run -d \
  --name sss-backend-dev \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://sss-postgres-dev:5432/bta_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_DATA_MONGODB_URI=mongodb://sss-mongo-dev:27017/sss_db \
  -v $(pwd)/BE:/app \
  -v $(pwd)/BE/gradle:/app/gradle \
  -v gradle-cache:/gradle_cache \
  --network sss-network \
  sss-backend-dev:latest

echo "백엔드 개발 컨테이너가 실행되었습니다."
echo "백엔드 API는 http://localhost:8080 에서 접근 가능합니다."
echo "소스 코드를 수정하면 자동으로 반영됩니다." 