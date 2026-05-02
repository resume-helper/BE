# Atomic CV — 인프라 설계 문서

> 작성일: 2026-05-01
> 목적: CI/CD 파이프라인 및 서버 인프라 설계 명세
> 배포 전략: Blue/Green (무중단 배포)

---

## 목차

1. [전체 아키텍처 개요](#1-전체-아키텍처-개요)
2. [서버 구성](#2-서버-구성)
3. [Blue/Green 배포 전략](#3-bluegreen-배포-전략)
4. [SSL 인증서 관리](#4-ssl-인증서-관리)
5. [환경변수 관리](#5-환경변수-관리)
6. [CI/CD 파이프라인](#6-cicd-파이프라인)
7. [배포 스크립트](#7-배포-스크립트)
8. [롤백 전략](#8-롤백-전략)

---

## 1. 전체 아키텍처 개요

```
[사용자]
    │ HTTPS (443)
    ▼
[Route53]  ──── 도메인 → EC2 Public IP
    │
    ▼
[EC2]
  └─ Nginx (Reverse Proxy + SSL Termination)
       ├─ Let's Encrypt (SSL 인증서)
       ├─ Blue  컨테이너 (8080) ← 현재 Active
       └─ Green 컨테이너 (8081) ← 신규 배포 대상
```

---

## 2. 서버 구성

### EC2

| 항목 | 내용 |
|------|------|
| **OS** | Amazon Linux 2023 |
| **설치 소프트웨어** | Docker, Docker Compose, Nginx, Certbot |
| **포트** | 22 (SSH), 80 (HTTP → HTTPS 리다이렉트), 443 (HTTPS), 8080/8081 (앱 내부) |
| **IAM Role** | SSM Parameter Store 읽기 권한 포함 |

### 보안 그룹 (Security Group)

| 포트 | 허용 대상 | 용도 |
|------|----------|------|
| 22 | 배포 서버 IP (GitHub Actions) | SSH 접속 |
| 80 | 0.0.0.0/0 | HTTP → HTTPS 리다이렉트 |
| 443 | 0.0.0.0/0 | HTTPS |
| 8080, 8081 | EC2 내부 (127.0.0.1) | 앱 컨테이너 (외부 노출 금지) |

---

## 3. Blue/Green 배포 전략

### 개념

```
배포 전:  Nginx → Blue(8080) [Active]   Green(8081) [Idle]
배포 중:  Nginx → Blue(8080) [Active]   Green(8081) [새 버전 기동 중]
전환 후:  Nginx → Blue(8080) [Idle]     Green(8081) [Active]
```

- 항상 두 포트 중 하나가 Active, 하나가 Idle 상태
- 신규 버전은 Idle 포트에 먼저 기동 → Health Check 통과 후 Nginx upstream 전환
- 전환 직후 이전 컨테이너 종료 (즉시 롤백 가능 시간 확보 가능)

### Nginx 설정 (`/etc/nginx/conf.d/atomiccv.conf`)

```nginx
upstream app {
    server 127.0.0.1:8080;  # Blue/Green 전환 시 포트 변경
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        proxy_pass http://app;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 4. SSL 인증서 관리

| 항목 | 내용 |
|------|------|
| **도구** | Certbot (Let's Encrypt) |
| **비용** | 무료 |
| **갱신** | 90일마다 자동 갱신 (cron 설정) |
| **설치 명령** | `certbot --nginx -d your-domain.com` |

### 자동 갱신 cron 설정

```bash
# /etc/cron.d/certbot
0 0 * * * root certbot renew --quiet && nginx -s reload
```

---

## 5. 환경변수 관리

### 구조

```
GitHub Secrets          → CI/CD 파이프라인 전용 (AWS 접근키, EC2 SSH 키 등)
AWS SSM Parameter Store → 앱 실행 환경변수 (DB, JWT, SMTP 등)
EC2 IAM Role            → EC2가 별도 키 없이 SSM에 직접 접근
```

### GitHub Secrets 목록

| Secret 이름 | 용도 |
|------------|------|
| `EC2_HOST` | EC2 Public IP |
| `EC2_USER` | EC2 접속 유저 (ec2-user) |
| `EC2_SSH_KEY` | EC2 SSH Private Key |
| `AWS_REGION` | AWS 리전 |

### SSM Parameter Store 경로 구조

```
/atomiccv/prod/DB_URL
/atomiccv/prod/DB_USERNAME
/atomiccv/prod/DB_PASSWORD
/atomiccv/prod/JWT_SECRET
/atomiccv/prod/JWT_ACCESS_EXPIRY
/atomiccv/prod/JWT_REFRESH_EXPIRY
/atomiccv/prod/REDIS_HOST
/atomiccv/prod/GOOGLE_CLIENT_ID
/atomiccv/prod/GOOGLE_CLIENT_SECRET
/atomiccv/prod/KAKAO_CLIENT_ID
/atomiccv/prod/KAKAO_CLIENT_SECRET
/atomiccv/prod/NAVER_CLIENT_ID
/atomiccv/prod/NAVER_CLIENT_SECRET
```

### EC2 IAM Role 정책 (최소 권한)

```json
{
  "Effect": "Allow",
  "Action": [
    "ssm:GetParameter",
    "ssm:GetParametersByPath"
  ],
  "Resource": "arn:aws:ssm:*:*:parameter/atomiccv/prod/*"
}
```

---

## 6. CI/CD 파이프라인

### 트리거

| 이벤트 | 동작 |
|--------|------|
| `main` 브랜치 PR 머지 | CI (빌드 + 테스트) + CD (자동 배포) |
| PR 생성 / 업데이트 | CI만 실행 (빌드 + 테스트) |

### GitHub Actions Workflow (`.github/workflows/deploy.yml`)

```yaml
name: CI/CD

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  ci:
    name: Build & Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build & Test
        run: ./gradlew build

  cd:
    name: Deploy
    runs-on: ubuntu-latest
    needs: ci
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker image
        run: docker build -t atomiccv:${{ github.sha }} .

      - name: Save Docker image
        run: docker save atomiccv:${{ github.sha }} | gzip > atomiccv.tar.gz

      - name: Copy image to EC2
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          source: "atomiccv.tar.gz,scripts/deploy.sh"
          target: "~/deploy"

      - name: Execute deploy script
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            chmod +x ~/deploy/scripts/deploy.sh
            ~/deploy/scripts/deploy.sh ${{ github.sha }}
```

---

## 7. 배포 스크립트

### `scripts/deploy.sh`

```bash
#!/bin/bash
set -e

IMAGE_TAG=$1
IMAGE_NAME="atomiccv:$IMAGE_TAG"

# 현재 Active 포트 확인
CURRENT_PORT=$(grep -oP '(?<=server 127.0.0.1:)\d+' /etc/nginx/conf.d/atomiccv.conf)
if [ "$CURRENT_PORT" == "8080" ]; then
  IDLE_PORT=8081
  IDLE_COLOR="green"
else
  IDLE_PORT=8080
  IDLE_COLOR="blue"
fi

echo "▶ 현재 Active: $CURRENT_PORT / 신규 배포 대상: $IDLE_PORT ($IDLE_COLOR)"

# Docker 이미지 로드
echo "▶ Docker 이미지 로드"
docker load < ~/deploy/atomiccv.tar.gz

# SSM에서 환경변수 로드
echo "▶ 환경변수 로드 (SSM)"
ENV_VARS=$(aws ssm get-parameters-by-path \
  --path "/atomiccv/prod/" \
  --with-decryption \
  --region $AWS_REGION \
  --query "Parameters[*].[Name,Value]" \
  --output text | awk '{split($1,a,"/"); printf "-e %s=%s ", a[4], $2}')

# Idle 포트에 새 컨테이너 기동
echo "▶ 새 컨테이너 기동 (포트: $IDLE_PORT)"
docker stop atomiccv-$IDLE_COLOR 2>/dev/null || true
docker rm atomiccv-$IDLE_COLOR 2>/dev/null || true
docker run -d \
  --name atomiccv-$IDLE_COLOR \
  -p $IDLE_PORT:8080 \
  $ENV_VARS \
  $IMAGE_NAME

# Health Check
echo "▶ Health Check 시작"
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:$IDLE_PORT/actuator/health)
  if [ "$STATUS" == "200" ]; then
    echo "✅ Health Check 통과 ($i번째 시도)"
    break
  fi
  if [ $i -eq 10 ]; then
    echo "❌ Health Check 실패 — 배포 중단"
    docker stop atomiccv-$IDLE_COLOR
    exit 1
  fi
  echo "⏳ 대기 중... ($i/10)"
  sleep 5
done

# Nginx upstream 전환
echo "▶ Nginx upstream 전환 → $IDLE_PORT"
sudo sed -i "s/server 127.0.0.1:$CURRENT_PORT/server 127.0.0.1:$IDLE_PORT/" /etc/nginx/conf.d/atomiccv.conf
sudo nginx -s reload

# 이전 컨테이너 종료
echo "▶ 이전 컨테이너 종료"
if [ "$IDLE_COLOR" == "green" ]; then
  docker stop atomiccv-blue 2>/dev/null || true
else
  docker stop atomiccv-green 2>/dev/null || true
fi

echo "🎉 배포 완료 — Active Port: $IDLE_PORT"
```

---

## 8. 롤백 전략

### 즉시 롤백 (이전 컨테이너 재활성화)

```bash
#!/bin/bash
# scripts/rollback.sh

CURRENT_PORT=$(grep -oP '(?<=server 127.0.0.1:)\d+' /etc/nginx/conf.d/atomiccv.conf)
if [ "$CURRENT_PORT" == "8080" ]; then
  ROLLBACK_PORT=8081
  ROLLBACK_COLOR="green"
else
  ROLLBACK_PORT=8080
  ROLLBACK_COLOR="blue"
fi

echo "▶ 롤백: $ROLLBACK_PORT ($ROLLBACK_COLOR) 재활성화"
docker start atomiccv-$ROLLBACK_COLOR
sudo sed -i "s/server 127.0.0.1:$CURRENT_PORT/server 127.0.0.1:$ROLLBACK_PORT/" /etc/nginx/conf.d/atomiccv.conf
sudo nginx -s reload

echo "✅ 롤백 완료 — Active Port: $ROLLBACK_PORT"
```

> 롤백은 이전 컨테이너가 종료되지 않고 stop 상태로 유지되어 있어야 합니다.
> 배포 스크립트에서 이전 컨테이너를 `docker stop`만 하고 `docker rm` 하지 않는 이유입니다.

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-05-01 | 최초 작성 |
