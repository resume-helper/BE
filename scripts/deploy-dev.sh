#!/bin/bash
set -e

IMAGE_TAG=$1
IMAGE_NAME="atomiccv:$IMAGE_TAG"
AWS_REGION="ap-northeast-2"
DEV_PORT=8080

echo "▶ Docker 이미지 로드"
docker load < ~/deploy/atomiccv.tar.gz

echo "▶ 환경변수 로드 (SSM /atomiccv/dev/)"
ENV_ARGS=""
while IFS=$'\t' read -r name value; do
  key=$(basename "$name")
  ENV_ARGS="$ENV_ARGS -e $key=$value"
done < <(aws ssm get-parameters-by-path \
  --path "/atomiccv/dev/" \
  --with-decryption \
  --region "$AWS_REGION" \
  --query "Parameters[*].[Name,Value]" \
  --output text)

echo "▶ Prod 컨테이너 중단"
docker stop atomiccv-blue 2>/dev/null || true
docker stop atomiccv-green 2>/dev/null || true

echo "▶ Dev 컨테이너 기동 (포트: $DEV_PORT)"
docker stop atomiccv-dev 2>/dev/null || true
docker rm atomiccv-dev 2>/dev/null || true
docker run -d \
  --name atomiccv-dev \
  -p "$DEV_PORT:8080" \
  -e SPRING_PROFILES_ACTIVE=dev \
  $ENV_ARGS \
  "$IMAGE_NAME"

echo "▶ Health Check 시작"
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$DEV_PORT/actuator/health" 2>/dev/null || echo "000")
  if [ "$STATUS" == "200" ]; then
    echo "✅ Health Check 통과 ($i번째 시도)"
    break
  fi
  if [ "$i" -eq 10 ]; then
    echo "❌ Health Check 실패 — 배포 중단"
    docker stop atomiccv-dev || true
    docker rm atomiccv-dev || true
    exit 1
  fi
  echo "⏳ 대기 중... ($i/10, 상태: $STATUS)"
  sleep 5
done

echo "🎉 Dev 서버 배포 완료"
