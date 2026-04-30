#!/bin/bash
set -e

IMAGE_TAG=$1
IMAGE_NAME="atomiccv:$IMAGE_TAG"
AWS_REGION="ap-northeast-2"

# 현재 Active 포트 확인
CURRENT_PORT=$(grep -oP '(?<=server 127.0.0.1:)\d+' /etc/nginx/conf.d/atomiccv.conf)
if [ "$CURRENT_PORT" == "8080" ]; then
  IDLE_PORT=8081
  IDLE_COLOR="green"
  ACTIVE_COLOR="blue"
else
  IDLE_PORT=8080
  IDLE_COLOR="blue"
  ACTIVE_COLOR="green"
fi

echo "▶ Active: $CURRENT_PORT ($ACTIVE_COLOR) → 배포 대상: $IDLE_PORT ($IDLE_COLOR)"

# Docker 이미지 로드
echo "▶ Docker 이미지 로드"
docker load < ~/deploy/atomiccv.tar.gz

# SSM에서 환경변수 로드
echo "▶ 환경변수 로드 (SSM Parameter Store)"
ENV_ARGS=""
while IFS=$'\t' read -r name value; do
  key=$(basename "$name")
  ENV_ARGS="$ENV_ARGS -e $key=$value"
done < <(aws ssm get-parameters-by-path \
  --path "/atomiccv/prod/" \
  --with-decryption \
  --region "$AWS_REGION" \
  --query "Parameters[*].[Name,Value]" \
  --output text)

# Idle 포트에 새 컨테이너 기동
echo "▶ 새 컨테이너 기동 (포트: $IDLE_PORT)"
docker stop "atomiccv-$IDLE_COLOR" 2>/dev/null || true
docker rm "atomiccv-$IDLE_COLOR" 2>/dev/null || true
docker run -d \
  --name "atomiccv-$IDLE_COLOR" \
  -p "$IDLE_PORT:8080" \
  $ENV_ARGS \
  "$IMAGE_NAME"

# Health Check (최대 50초 대기)
echo "▶ Health Check 시작"
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$IDLE_PORT/actuator/health" 2>/dev/null || echo "000")
  if [ "$STATUS" == "200" ]; then
    echo "✅ Health Check 통과 ($i번째 시도)"
    break
  fi
  if [ "$i" -eq 10 ]; then
    echo "❌ Health Check 실패 — 배포 중단"
    docker stop "atomiccv-$IDLE_COLOR" || true
    docker rm "atomiccv-$IDLE_COLOR" || true
    exit 1
  fi
  echo "⏳ 대기 중... ($i/10, 상태: $STATUS)"
  sleep 5
done

# Nginx upstream 전환
echo "▶ Nginx upstream 전환 → $IDLE_PORT"
sudo sed -i "s/server 127.0.0.1:$CURRENT_PORT/server 127.0.0.1:$IDLE_PORT/" /etc/nginx/conf.d/atomiccv.conf
sudo nginx -s reload

# 이전 컨테이너 중지 (롤백 대비 rm 안 함)
echo "▶ 이전 컨테이너 중지"
docker stop "atomiccv-$ACTIVE_COLOR" 2>/dev/null || true

echo "🎉 배포 완료 — Active Port: $IDLE_PORT ($IDLE_COLOR)"
