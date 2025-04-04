#!/bin/bash

echo "===== 개발 환경 재시작 스크립트 시작 ====="
echo "$(date)"

echo "1. 기존 컨테이너 중지 및 삭제 중..."
docker compose -f docker-compose.dev.yml down
echo "✓ 기존 컨테이너 중지 및 삭제 완료"

echo "2. 개발 환경으로 컨테이너 빌드 및 시작 중..."
echo "   (이 작업은 몇 분 정도 소요될 수 있습니다)"
docker compose -f docker-compose.dev.yml up --build
echo "✓ 개발 환경 컨테이너 실행 완료"

echo "===== 개발 환경 재시작 스크립트 종료 ====="

# Windows에서 실행 시 필요한 CRLF 문제 예방을 위한 주석
# git config --global core.autocrlf false 명령어로 개행 문자 설정 변경 권장 