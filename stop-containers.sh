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

# 모든 컨테이너 중지 및 삭제
if [ "$1" = "all" ]; then
  echo "모든 프로젝트 컨테이너를 중지하고 삭제합니다..."
  stop_and_remove_container "sss-frontend"
  stop_and_remove_container "sss-backend"
  stop_and_remove_container "sss-postgres"
  stop_and_remove_container "sss-frontend-dev"
  stop_and_remove_container "sss-backend-dev"
  stop_and_remove_container "sss-postgres-dev"
  echo "모든 컨테이너가 삭제되었습니다."
  exit 0
fi

# 배포 환경 컨테이너 중지 및 삭제
if [ "$1" = "prod" ]; then
  echo "배포 환경 컨테이너를 중지하고 삭제합니다..."
  stop_and_remove_container "sss-frontend"
  stop_and_remove_container "sss-backend"
  stop_and_remove_container "sss-postgres"
  echo "배포 환경 컨테이너가 삭제되었습니다."
  exit 0
fi

# 개발 환경 컨테이너 중지 및 삭제
if [ "$1" = "dev" ]; then
  echo "개발 환경 컨테이너를 중지하고 삭제합니다..."
  stop_and_remove_container "sss-frontend-dev"
  stop_and_remove_container "sss-backend-dev"
  stop_and_remove_container "sss-postgres-dev"
  echo "개발 환경 컨테이너가 삭제되었습니다."
  exit 0
fi

# 백엔드 컨테이너 중지 및 삭제
if [ "$1" = "backend" ] || [ "$1" = "be" ]; then
  if [ "$2" = "dev" ]; then
    stop_and_remove_container "sss-backend-dev"
  else
    stop_and_remove_container "sss-backend"
  fi
  exit 0
fi

# 프론트엔드 컨테이너 중지 및 삭제
if [ "$1" = "frontend" ] || [ "$1" = "fe" ]; then
  if [ "$2" = "dev" ]; then
    stop_and_remove_container "sss-frontend-dev"
  else
    stop_and_remove_container "sss-frontend"
  fi
  exit 0
fi

# 데이터베이스 컨테이너 중지 및 삭제
if [ "$1" = "database" ] || [ "$1" = "db" ]; then
  if [ "$2" = "dev" ]; then
    stop_and_remove_container "sss-postgres-dev"
  else
    stop_and_remove_container "sss-postgres"
  fi
  exit 0
fi

# 사용법 출력
echo "사용법: $0 [all|prod|dev|backend|frontend|database] [dev]"
echo ""
echo "옵션:"
echo "  all       - 모든 컨테이너 중지 및 삭제"
echo "  prod      - 배포 환경 컨테이너 중지 및 삭제"
echo "  dev       - 개발 환경 컨테이너 중지 및 삭제"
echo "  backend   - 백엔드 컨테이너 중지 및 삭제 (dev 옵션 추가 시 개발 환경)"
echo "  frontend  - 프론트엔드 컨테이너 중지 및 삭제 (dev 옵션 추가 시 개발 환경)"
echo "  database  - 데이터베이스 컨테이너 중지 및 삭제 (dev 옵션 추가 시 개발 환경)"
echo ""
echo "예시:"
echo "  $0 all              - 모든 컨테이너 중지 및 삭제"
echo "  $0 backend dev      - 개발 환경 백엔드 컨테이너 중지 및 삭제"
echo "  $0 frontend         - 배포 환경 프론트엔드 컨테이너 중지 및 삭제" 