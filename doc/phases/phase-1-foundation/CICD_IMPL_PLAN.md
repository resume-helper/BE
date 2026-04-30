# CI/CD 인프라 구축 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** GitHub Actions + EC2 Blue/Green 무중단 배포 파이프라인 구축

**Architecture:** EC2 단일 서버에 Nginx를 리버스 프록시로 두고, 8080/8081 포트를 Blue/Green으로 교차 배포. Let's Encrypt로 HTTPS 처리, AWS SSM Parameter Store로 환경변수 관리.

**Tech Stack:** GitHub Actions, Docker, Nginx, Certbot (Let's Encrypt), AWS EC2, AWS SSM Parameter Store, Spring Boot 3.x + Kotlin

---

## 파일 구조

```
프로젝트 루트
├── .github/
│   └── workflows/
│       └── deploy.yml          # CI/CD 워크플로우
├── Dockerfile                  # 앱 컨테이너 이미지 정의
├── scripts/
│   ├── deploy.sh               # Blue/Green 배포 스크립트
│   └── rollback.sh             # 롤백 스크립트
└── doc/
    └── INFRA_DESIGN.md         # 인프라 설계 문서 (기작성)

EC2 서버
├── /etc/nginx/conf.d/
│   └── atomiccv.conf           # Nginx 설정
└── /etc/cron.d/
    └── certbot                 # SSL 자동 갱신 cron
```

---

## Task 1: EC2 서버 초기 환경 구성

**Files:**
- EC2 서버에서 직접 실행 (파일 생성 없음)

- [ ] **Step 1: EC2 접속 후 패키지 업데이트**

```bash
sudo dnf update -y
```

- [ ] **Step 2: Docker 설치**

```bash
sudo dnf install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
# 재접속 후 docker 명령어 sudo 없이 사용 가능
```

- [ ] **Step 3: Docker 설치 확인**

```bash
docker --version
```
Expected: `Docker version 24.x.x` 이상

- [ ] **Step 4: Nginx 설치**

```bash
sudo dnf install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

- [ ] **Step 5: Nginx 설치 확인**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost
```
Expected: `200`

- [ ] **Step 6: AWS CLI 설치 확인 (Amazon Linux는 기본 포함)**

```bash
aws --version
```
Expected: `aws-cli/2.x.x` 이상

---

## Task 2: EC2 IAM Role 설정

**Files:**
- AWS 콘솔에서 설정 (파일 생성 없음)

- [ ] **Step 1: AWS 콘솔 → IAM → 역할 → 역할 생성**

```
신뢰할 수 있는 엔터티: AWS 서비스 → EC2
```

- [ ] **Step 2: 인라인 정책 추가**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParametersByPath"
      ],
      "Resource": "arn:aws:ssm:*:*:parameter/atomiccv/prod/*"
    }
  ]
}
```

- [ ] **Step 3: EC2 인스턴스에 IAM Role 연결**

```
AWS 콘솔 → EC2 → 인스턴스 선택 → 작업 → 보안 → IAM 역할 수정 → 생성한 역할 선택
```

- [ ] **Step 4: EC2에서 SSM 접근 확인**

```bash
aws ssm get-parameters-by-path \
  --path "/atomiccv/prod/" \
  --region ap-northeast-2
```
Expected: `{"Parameters": []}` (아직 등록 전이므로 빈 배열)

---

## Task 3: AWS SSM Parameter Store 환경변수 등록

**Files:**
- AWS 콘솔 또는 CLI에서 설정

- [ ] **Step 1: DB 관련 파라미터 등록**

```bash
aws ssm put-parameter \
  --name "/atomiccv/prod/DB_URL" \
  --value "jdbc:mysql://<RDS_ENDPOINT>:3306/atomiccv" \
  --type "SecureString" \
  --region ap-northeast-2

aws ssm put-parameter \
  --name "/atomiccv/prod/DB_USERNAME" \
  --value "<DB_USERNAME>" \
  --type "SecureString" \
  --region ap-northeast-2

aws ssm put-parameter \
  --name "/atomiccv/prod/DB_PASSWORD" \
  --value "<DB_PASSWORD>" \
  --type "SecureString" \
  --region ap-northeast-2
```

- [ ] **Step 2: JWT 관련 파라미터 등록**

```bash
aws ssm put-parameter \
  --name "/atomiccv/prod/JWT_SECRET" \
  --value "<JWT_SECRET_32자_이상>" \
  --type "SecureString" \
  --region ap-northeast-2

aws ssm put-parameter \
  --name "/atomiccv/prod/JWT_ACCESS_EXPIRY" \
  --value "3600000" \
  --type "String" \
  --region ap-northeast-2

aws ssm put-parameter \
  --name "/atomiccv/prod/JWT_REFRESH_EXPIRY" \
  --value "604800000" \
  --type "String" \
  --region ap-northeast-2
```

- [ ] **Step 3: Redis 파라미터 등록**

```bash
aws ssm put-parameter \
  --name "/atomiccv/prod/REDIS_HOST" \
  --value "127.0.0.1" \
  --type "String" \
  --region ap-northeast-2

aws ssm put-parameter \
  --name "/atomiccv/prod/REDIS_PORT" \
  --value "6379" \
  --type "String" \
  --region ap-northeast-2
```

- [ ] **Step 4: SMTP 파라미터 등록**

```bash
aws ssm put-parameter \
  --name "/atomiccv/prod/SMTP_USERNAME" \
  --value "<GMAIL_ADDRESS>" \
  --type "SecureString" \
  --region ap-northeast-2

aws ssm put-parameter \
  --name "/atomiccv/prod/SMTP_PASSWORD" \
  --value "<GMAIL_APP_PASSWORD>" \
  --type "SecureString" \
  --region ap-northeast-2
```

- [ ] **Step 5: 등록 확인**

```bash
aws ssm get-parameters-by-path \
  --path "/atomiccv/prod/" \
  --with-decryption \
  --region ap-northeast-2 \
  --query "Parameters[*].Name"
```
Expected: 9개 파라미터 이름 목록 출력

---

## Task 4: SSL 인증서 설정 (Let's Encrypt)

**Files:**
- `/etc/nginx/conf.d/atomiccv.conf` (Certbot이 자동 수정)
- `/etc/cron.d/certbot`

- [ ] **Step 1: Route53에서 A 레코드 등록**

```
AWS 콘솔 → Route53 → 호스팅 영역 → A 레코드 추가
값: EC2 Public IP
```

- [ ] **Step 2: Certbot 설치**

```bash
sudo dnf install -y python3-certbot-nginx
```

- [ ] **Step 3: 임시 Nginx 설정 (도메인 확인용)**

`/etc/nginx/conf.d/atomiccv.conf` 파일 생성:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    location / {
        return 200 'ok';
    }
}
```

```bash
sudo nginx -t && sudo nginx -s reload
```

- [ ] **Step 4: SSL 인증서 발급**

```bash
sudo certbot --nginx -d your-domain.com
```
이메일 입력, 약관 동의, HTTP→HTTPS 리다이렉트 선택(2번)

- [ ] **Step 5: 인증서 발급 확인**

```bash
curl -s -o /dev/null -w "%{http_code}" https://your-domain.com
```
Expected: `200`

- [ ] **Step 6: 자동 갱신 cron 설정**

```bash
echo "0 0 * * * root certbot renew --quiet && nginx -s reload" \
  | sudo tee /etc/cron.d/certbot
```

---

## Task 5: Nginx Blue/Green 설정

**Files:**
- Modify: `/etc/nginx/conf.d/atomiccv.conf`

- [ ] **Step 1: Nginx 설정 파일 작성**

```bash
sudo tee /etc/nginx/conf.d/atomiccv.conf > /dev/null << 'EOF'
upstream app {
    server 127.0.0.1:8080;
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
        proxy_read_timeout 60s;
    }
}
EOF
```

- [ ] **Step 2: Nginx 설정 유효성 검사 및 재시작**

```bash
sudo nginx -t && sudo nginx -s reload
```
Expected: `nginx: configuration file /etc/nginx/nginx.conf test is successful`

---

## Task 6: Redis 로컬 설치

**Files:**
- EC2 서버에서 직접 실행

- [ ] **Step 1: Redis 컨테이너 실행**

```bash
docker run -d \
  --name redis \
  --restart always \
  -p 127.0.0.1:6379:6379 \
  redis:7-alpine
```

- [ ] **Step 2: Redis 동작 확인**

```bash
docker exec -it redis redis-cli ping
```
Expected: `PONG`

---

## Task 7: Dockerfile 작성

**Files:**
- Create: `Dockerfile` (프로젝트 루트)

- [ ] **Step 1: Dockerfile 작성**

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

- [ ] **Step 2: 로컬에서 빌드 후 이미지 생성 테스트**

```bash
./gradlew build -x test
docker build -t atomiccv:test .
docker images | grep atomiccv
```
Expected: `atomiccv   test   ...` 이미지 존재

- [ ] **Step 3: 커밋**

```bash
git add Dockerfile
git commit -m "chore: add Dockerfile for production build"
```

---

## Task 8: 배포 스크립트 작성

**Files:**
- Create: `scripts/deploy.sh`
- Create: `scripts/rollback.sh`

- [ ] **Step 1: scripts 디렉토리 생성**

```bash
mkdir -p scripts
```

- [ ] **Step 2: deploy.sh 작성**

`scripts/deploy.sh`:

```bash
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
    echo "❌ Health Check 실패 — 배포 중단 및 컨테이너 제거"
    docker stop "atomiccv-$IDLE_COLOR" || true
    docker rm "atomiccv-$IDLE_COLOR" || true
    exit 1
  fi
  echo "⏳ 대기 중... ($i/10, 현재 상태: $STATUS)"
  sleep 5
done

# Nginx upstream 전환
echo "▶ Nginx upstream 전환 → $IDLE_PORT"
sudo sed -i "s/server 127.0.0.1:$CURRENT_PORT/server 127.0.0.1:$IDLE_PORT/" /etc/nginx/conf.d/atomiccv.conf
sudo nginx -s reload

# 이전 컨테이너 중지 (rm 하지 않음 — 롤백 대비)
echo "▶ 이전 컨테이너 중지 (롤백 대비 유지)"
docker stop "atomiccv-$ACTIVE_COLOR" 2>/dev/null || true

echo "🎉 배포 완료 — Active Port: $IDLE_PORT ($IDLE_COLOR)"
```

- [ ] **Step 3: rollback.sh 작성**

`scripts/rollback.sh`:

```bash
#!/bin/bash
set -e

CURRENT_PORT=$(grep -oP '(?<=server 127.0.0.1:)\d+' /etc/nginx/conf.d/atomiccv.conf)
if [ "$CURRENT_PORT" == "8080" ]; then
  ROLLBACK_PORT=8081
  ROLLBACK_COLOR="green"
  CURRENT_COLOR="blue"
else
  ROLLBACK_PORT=8080
  ROLLBACK_COLOR="blue"
  CURRENT_COLOR="green"
fi

echo "▶ 롤백: $ROLLBACK_PORT ($ROLLBACK_COLOR) 컨테이너 재시작"
docker start "atomiccv-$ROLLBACK_COLOR"

# Health Check
for i in {1..10}; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$ROLLBACK_PORT/actuator/health" 2>/dev/null || echo "000")
  if [ "$STATUS" == "200" ]; then
    echo "✅ Health Check 통과"
    break
  fi
  if [ "$i" -eq 10 ]; then
    echo "❌ 롤백 대상 컨테이너도 Health Check 실패"
    exit 1
  fi
  sleep 5
done

# Nginx 전환
sudo sed -i "s/server 127.0.0.1:$CURRENT_PORT/server 127.0.0.1:$ROLLBACK_PORT/" /etc/nginx/conf.d/atomiccv.conf
sudo nginx -s reload

docker stop "atomiccv-$CURRENT_COLOR" 2>/dev/null || true

echo "✅ 롤백 완료 — Active Port: $ROLLBACK_PORT ($ROLLBACK_COLOR)"
```

- [ ] **Step 4: 실행 권한 부여**

```bash
chmod +x scripts/deploy.sh scripts/rollback.sh
```

- [ ] **Step 5: 커밋**

```bash
git add scripts/
git commit -m "chore: add blue/green deploy and rollback scripts"
```

---

## Task 9: GitHub Actions Workflow 작성

**Files:**
- Create: `.github/workflows/deploy.yml`

- [ ] **Step 1: .github/workflows 디렉토리 생성**

```bash
mkdir -p .github/workflows
```

- [ ] **Step 2: GitHub Secrets 등록 (GitHub 콘솔)**

```
Settings → Secrets and variables → Actions → New repository secret

EC2_HOST    : EC2 Public IP
EC2_USER    : ec2-user
EC2_SSH_KEY : EC2 SSH Private Key 전체 내용 (-----BEGIN RSA PRIVATE KEY----- 포함)
AWS_REGION  : ap-northeast-2
```

- [ ] **Step 3: deploy.yml 작성**

`.github/workflows/deploy.yml`:

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
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build & Test
        run: ./gradlew build

  cd:
    name: Deploy to EC2
    runs-on: ubuntu-latest
    needs: ci
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build (skip test — already ran in ci job)
        run: ./gradlew build -x test

      - name: Build Docker image
        run: docker build -t atomiccv:${{ github.sha }} .

      - name: Save Docker image as tar.gz
        run: docker save atomiccv:${{ github.sha }} | gzip > atomiccv.tar.gz

      - name: Copy files to EC2
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          source: "atomiccv.tar.gz,scripts/deploy.sh"
          target: "~/deploy"

      - name: Execute deploy script on EC2
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            chmod +x ~/deploy/scripts/deploy.sh
            ~/deploy/scripts/deploy.sh ${{ github.sha }}
```

- [ ] **Step 4: 커밋 및 push**

```bash
git add .github/workflows/deploy.yml
git commit -m "ci: add GitHub Actions CI/CD workflow"
git push origin main
```

- [ ] **Step 5: GitHub Actions 실행 확인**

```
GitHub → Actions 탭 → CI/CD workflow → 실행 로그 확인
```
Expected: Build & Test ✅, Deploy to EC2 ✅

---

## Task 10: Spring Boot Actuator 설정 확인

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/application-prod.yml`

- [ ] **Step 1: Actuator 의존성 확인**

`build.gradle.kts`에 아래 의존성이 있는지 확인:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
```

없으면 추가 후:

```bash
./gradlew dependencies | grep actuator
```
Expected: `spring-boot-starter-actuator` 포함

- [ ] **Step 2: prod 프로파일 Actuator 설정**

`src/main/resources/application-prod.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

- [ ] **Step 3: 커밋**

```bash
git add build.gradle.kts src/main/resources/application-prod.yml
git commit -m "chore: expose actuator health endpoint for deployment health check"
```

---

## Task 11: 첫 배포 검증

- [ ] **Step 1: main 브랜치에 push 후 Actions 모니터링**

```bash
git push origin main
# GitHub Actions → 실행 중인 워크플로우 확인
```

- [ ] **Step 2: 배포 완료 후 Health Check**

```bash
curl https://your-domain.com/actuator/health
```
Expected: `{"status":"UP"}`

- [ ] **Step 3: HTTPS 리다이렉트 확인**

```bash
curl -s -o /dev/null -w "%{http_code}" http://your-domain.com
```
Expected: `301`

- [ ] **Step 4: 롤백 스크립트 동작 검증 (선택)**

```bash
ssh ec2-user@<EC2_HOST> "~/deploy/scripts/rollback.sh"
```
Expected: `✅ 롤백 완료` 출력 후 이전 컨테이너로 트래픽 전환
