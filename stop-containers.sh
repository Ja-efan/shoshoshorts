#!/bin/bash

# 컨테이너 중지 및 삭제 함수
stop_and_remove_container() {
  local container_name=$1
  if docker ps -a | grep -q $container_name; then
    echo "$container_name 컨테이너를 중지하고 삭제합니다..."
    docker stop $container_name >/dev/null 2>&1
    docker rm $container_name >/dev/null 2>&1
    echo "$container_name 컨테이너가 삭제되었습니다."
  else
    echo "$container_name 컨테이너가 존재하지 않습니다."
  fi
}

# Docker Compose를 사용하여 모든 개발 환경 컨테이너 중지
stop_all_dev_containers() {
  echo "Docker Compose를 사용하여 모든 개발 환경 컨테이너를 중지합니다..."
  
  # 볼륨 삭제 여부 확인
  read -p "Docker 볼륨도 함께 삭제하시겠습니까? (데이터가 모두 삭제됩니다) [y/N]: " delete_volumes
  
  if [[ "$delete_volumes" =~ ^[Yy]$ ]]; then
    echo "컨테이너와 볼륨을 모두 삭제합니다..."
    docker compose -f docker-compose.dev.yml down -v
    echo "모든 개발 환경 컨테이너와 볼륨이 삭제되었습니다."
  else
    echo "볼륨은 유지하고 컨테이너만 삭제합니다..."
    docker compose -f docker-compose.dev.yml down
    echo "모든 개발 환경 컨테이너가 중지되었습니다. 볼륨은 유지됩니다."
  fi
}

# 개발 환경 컨테이너 중지 및 삭제
if [ "$1" = "all" ] || [ "$1" = "dev" ] || [ -z "$1" ]; then
  stop_all_dev_containers
  exit 0
fi

# 백엔드 컨테이너 중지 및 삭제
if [ "$1" = "backend" ] || [ "$1" = "be" ]; then
  stop_and_remove_container "sss-backend-dev"
  exit 0
fi

# 프론트엔드 컨테이너 중지 및 삭제
if [ "$1" = "frontend" ] || [ "$1" = "fe" ]; then
  stop_and_remove_container "sss-frontend-dev"
  exit 0
fi

# PostgreSQL 데이터베이스 컨테이너 중지 및 삭제
if [ "$1" = "postgres" ] || [ "$1" = "db" ]; then
  stop_and_remove_container "sss-postgres-dev"
  
  # PostgreSQL 볼륨 삭제 여부 확인
  read -p "PostgreSQL 볼륨도 함께 삭제하시겠습니까? (모든 데이터가 삭제됩니다) [y/N]: " delete_volumes
  
  if [[ "$delete_volumes" =~ ^[Yy]$ ]]; then
    echo "PostgreSQL 볼륨을 삭제합니다..."
    docker volume rm postgres-data-dev
    echo "PostgreSQL 볼륨이 삭제되었습니다."
  else
    echo "PostgreSQL 볼륨은 유지됩니다."
  fi
  
  exit 0
fi

# MongoDB 데이터베이스 컨테이너 중지 및 삭제
if [ "$1" = "mongo" ]; then
  stop_and_remove_container "sss-mongo-dev"
  
  # MongoDB 볼륨 삭제 여부 확인
  read -p "MongoDB 볼륨도 함께 삭제하시겠습니까? (모든 데이터가 삭제됩니다) [y/N]: " delete_volumes
  
  if [[ "$delete_volumes" =~ ^[Yy]$ ]]; then
    echo "MongoDB 볼륨을 삭제합니다..."
    docker volume rm mongo-data-dev
    echo "MongoDB 볼륨이 삭제되었습니다."
  else
    echo "MongoDB 볼륨은 유지됩니다."
  fi
  
  exit 0
fi

# Gradle 캐시 볼륨 삭제
if [ "$1" = "gradle" ] || [ "$1" = "cache" ]; then
  # Gradle 캐시 볼륨 삭제 여부 확인
  read -p "Gradle 캐시 볼륨을 삭제하시겠습니까? [y/N]: " delete_volumes
  
  if [[ "$delete_volumes" =~ ^[Yy]$ ]]; then
    echo "Gradle 캐시 볼륨을 삭제합니다..."
    docker volume rm gradle-cache
    echo "Gradle 캐시 볼륨이 삭제되었습니다."
  else
    echo "작업이 취소되었습니다."
  fi
  
  exit 0
fi

# 사용법 출력
echo "사용법: $0 [all|dev|backend|frontend|postgres|mongo|gradle]"
echo ""
echo "옵션:"
echo "  all       - 모든 개발 환경 컨테이너 중지 (볼륨 삭제 여부 선택)"
echo "  dev       - 모든 개발 환경 컨테이너 중지 (all과 동일)"
echo "  backend   - 백엔드 개발 컨테이너 중지 및 삭제"
echo "  frontend  - 프론트엔드 개발 컨테이너 중지 및 삭제"
echo "  postgres  - PostgreSQL 개발 데이터베이스 컨테이너 중지 및 삭제 (볼륨 삭제 여부 선택)"
echo "  mongo     - MongoDB 개발 데이터베이스 컨테이너 중지 및 삭제 (볼륨 삭제 여부 선택)"
echo "  gradle    - Gradle 캐시 볼륨 삭제"
echo ""
echo "예시:"
echo "  $0                  - 모든 개발 환경 컨테이너 중지"
echo "  $0 all              - 모든 개발 환경 컨테이너 중지"
echo "  $0 backend          - 백엔드 개발 컨테이너 중지 및 삭제"
echo "  $0 postgres         - PostgreSQL 개발 컨테이너 중지 및 삭제 (볼륨 삭제 여부 선택)"
echo "  $0 gradle           - Gradle 캐시 볼륨 삭제" 