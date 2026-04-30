# Phase 1 CI/CD 구축 — 트러블슈팅 기록

> 작성일: 2026-05-01
> 작업 범위: EC2 서버 초기 설정 → GitHub Actions CI/CD → 첫 배포 검증

---

## 목차

1. [SSH 접속 실패 — EC2 유저명 오류](#1-ssh-접속-실패--ec2-유저명-오류)
2. [GitHub Actions workflow 파일 push 실패 — 토큰 권한 부족](#2-github-actions-workflow-파일-push-실패--토큰-권한-부족)
3. [CI 빌드 실패 — contextLoads 테스트 DB 연결 오류](#3-ci-빌드-실패--contextloads-테스트-db-연결-오류)
4. [CD SCP 타임아웃 — 보안 그룹 포트 22 제한](#4-cd-scp-타임아웃--보안-그룹-포트-22-제한)
5. [앱 기동 실패 — application-prod.yaml 누락](#5-앱-기동-실패--application-prodyaml-누락)
6. [Health Check 503 — Redis 연결 실패](#6-health-check-503--redis-연결-실패)

---

## 1. SSH 접속 실패 — EC2 유저명 오류

### 문제
```
ec2-user@3.37.55.10: Permission denied (publickey)
```
PEM 키가 있음에도 SSH 접속이 거부됨.

### 원인
EC2 AMI가 **Amazon Linux가 아닌 Ubuntu**였음.
- Amazon Linux: `ec2-user`
- Ubuntu: `ubuntu`
- CentOS: `centos`

기본 유저명이 AMI별로 다름.

### 해결
```bash
# ubuntu로 접속 성공
ssh -i resume-dev.pem ubuntu@3.37.55.10
```
이후 모든 SSH/SCP 설정을 `ubuntu` 유저로 변경.

---

## 2. GitHub Actions workflow 파일 push 실패 — 토큰 권한 부족

### 문제
```
refusing to allow an OAuth App to create or update workflow
`.github/workflows/deploy.yml` without `workflow` scope
```
`git push` 시 `.github/workflows/` 경로 파일이 거부됨.

### 원인
GitHub CLI 인증 토큰에 `workflow` 스코프가 없었음.
기본 gh 인증은 `repo`, `read:org` 등만 포함.

### 해결
```bash
gh auth refresh -h github.com -s workflow
# 브라우저 인증 완료 후 재 push
git push origin main
```

---

## 3. CI 빌드 실패 — contextLoads 테스트 DB 연결 오류

### 문제
```
HelperApplicationTests > contextLoads() FAILED
Caused by: DataSourceProperties$DataSourceBeanCreationException
Failed to configure a DataSource: 'url' attribute is not specified
```
GitHub Actions CI 환경에서 Spring Boot 통합 테스트 실패.

### 원인
`@SpringBootTest`는 전체 Spring Context를 로드하는데, CI 환경에는 MySQL/Redis가 없어 DataSource 초기화에 실패.

### 해결

**1. H2 인메모리 DB 의존성 추가** (`build.gradle.kts`)
```kotlin
testRuntimeOnly("com.h2database:h2")
```

**2. 테스트 전용 설정 파일 추가** (`src/test/resources/application-test.yaml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
```

**3. 테스트 클래스에 프로파일 지정**
```kotlin
@SpringBootTest
@ActiveProfiles("test")
class HelperApplicationTests { ... }
```

---

## 4. CD SCP 타임아웃 — 보안 그룹 포트 22 제한

### 문제
```
dial tcp ***:22: i/o timeout
```
GitHub Actions Runner에서 EC2로 파일 전송(SCP) 시 타임아웃.

### 원인
EC2 보안 그룹에서 포트 22(SSH)가 **개발자 로컬 IP만 허용**되어 있었음.
GitHub Actions Runner는 매번 다른 IP를 사용하므로 접근 불가.

```
기존 규칙: 22/tcp → 175.197.85.146/32 (개발자 IP)
```

### 해결
0.0.0.0/0 전체 오픈 대신 **동적 보안 그룹** 방식 채택.
배포 시 Runner IP만 임시 허용, 완료 후 즉시 제거.

```yaml
# GitHub Actions workflow
- name: Add runner IP to security group
  run: |
    RUNNER_IP=$(curl -s https://checkip.amazonaws.com)
    echo "RUNNER_IP=$RUNNER_IP" >> $GITHUB_ENV
    aws ec2 authorize-security-group-ingress \
      --group-id ${{ secrets.SECURITY_GROUP_ID }} \
      --protocol tcp --port 22 --cidr "$RUNNER_IP/32"

# ... 배포 스텝 ...

- name: Remove runner IP from security group
  if: always()    # 성공/실패 관계없이 항상 실행
  run: |
    aws ec2 revoke-security-group-ingress \
      --group-id ${{ secrets.SECURITY_GROUP_ID }} \
      --protocol tcp --port 22 --cidr "$RUNNER_IP/32"
```

보안 그룹 제어를 위해 GitHub Secrets에 AWS 자격증명 추가:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `SECURITY_GROUP_ID`

---

## 5. 앱 기동 실패 — application-prod.yaml 누락

### 문제
```
Failed to configure a DataSource: 'url' attribute is not specified
Reason: Failed to determine a suitable driver class
```
Docker 컨테이너 기동 시 Spring Boot 앱이 즉시 종료.

### 원인
Dockerfile에 `-Dspring.profiles.active=prod`로 지정했으나, `application-prod.yaml`이 존재하지 않았음.

`application.yaml`에는 로컬 개발용 하드코딩 값이 있고, SSM에서 주입되는 환경변수(`DB_URL`, `DB_USERNAME` 등)를 참조하는 설정이 없었음.

### 해결
`src/main/resources/application-prod.yaml` 신규 생성:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      ssl:
        enabled: true
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

SSM Parameter Store에서 주입되는 환경변수를 `${VAR_NAME}` 형태로 참조.

---

## 6. Health Check 503 — Redis 연결 실패

### 문제
```json
{
  "status": "DOWN",
  "components": {
    "redis": {
      "status": "DOWN",
      "details": {
        "error": "org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis"
      }
    }
  }
}
```
앱은 정상 기동되나 `/actuator/health`가 503 반환.

### 원인 (2가지 복합)

**원인 1: ElastiCache 보안 그룹이 EC2 접근 차단**

ElastiCache 보안 그룹(`sg-0c78a38ae561e1ca3`)의 인바운드 규칙이 개발자 로컬 IP만 허용.
EC2의 보안 그룹(`sg-087a01ca4101c7d79`)에서 오는 트래픽이 차단됨.

**원인 2: ElastiCache TLS 활성화**

```json
{ "TransitEncryptionEnabled": true }
```
ElastiCache가 TLS 암호화를 요구하지만, Spring Boot Redis 설정에 SSL이 비활성화 상태였음.
일반 TCP 연결(plain text)로 시도하여 연결 거부.

### 해결

**1. ElastiCache 보안 그룹에 EC2 보안 그룹 인바운드 추가**
```bash
aws ec2 authorize-security-group-ingress \
  --group-id sg-0c78a38ae561e1ca3 \
  --protocol tcp \
  --port 6379 \
  --source-group sg-087a01ca4101c7d79 \
  --region ap-northeast-2
```
특정 IP가 아닌 보안 그룹을 소스로 지정해 EC2 스케일아웃 시에도 자동 적용.

**2. SSM Redis 호스트를 Primary 엔드포인트로 변경**
```
replica.resume-redis.bjb21m.apn2.cache.amazonaws.com  →  (제거)
master.resume-redis.bjb21m.apn2.cache.amazonaws.com   →  (사용)
```

**3. application-prod.yaml에 SSL 설정 추가**
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      ssl:
        enabled: true
```

---

## 최종 결과

| 항목 | 결과 |
|------|------|
| CI (Build & Test) | ✅ H2 + Redis AutoConfig 제외로 통과 |
| CD (Deploy to EC2) | ✅ Blue/Green 첫 배포 성공 |
| Health Check | `{"status":"UP"}` ✅ |
| 보안 | 포트 22: 동적 허용/제거 / ElastiCache: 보안 그룹 기반 접근 제어 |
