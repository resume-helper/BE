# Phase 1-2: 프로젝트 초기 설정 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kotlin + Gradle 멀티모듈 Spring Boot 프로젝트 초기 구조 완성

**Architecture:** Modular Monolith (DDD, Hexagonal). 모듈별 독립 빌드·테스트 가능 구조. 모듈 간 통신은 Port Interface 또는 Domain Event만 허용.

**Tech Stack:** Kotlin, Spring Boot 3.x, Gradle (Kotlin DSL), ktlint, detekt, AWS SSM Parameter Store

**참고 문서:**
- `doc/MODULE_STRUCTURE.md` — 모듈 구성·패키지 구조·의존성 방향
- `doc/discussion.md` [5] — 로컬 개발 환경 (DB: AWS RDB, Redis: 로컬)
- `doc/discussion.md` [12] — 코드 컨벤션 (ktlint, detekt, pre-commit)
- `doc/INFRA_DESIGN.md` — SSM Parameter Store 경로

---

## 파일 구조

```
프로젝트 루트
├── settings.gradle.kts              # 멀티모듈 선언
├── build.gradle.kts                 # 루트 공통 설정 (ktlint, detekt, 공통 의존성)
├── gradle.properties                # Kotlin, JVM 버전 등
├── .editorconfig                    # ktlint 스타일 기준
├── detekt.yml                       # detekt 정적 분석 설정
├── .githooks/
│   └── pre-commit                   # ktlint 검사 스크립트
├── app/
│   └── build.gradle.kts
├── module-shared/
│   └── build.gradle.kts
├── module-auth/
│   └── build.gradle.kts
├── module-resume/
│   └── build.gradle.kts
├── module-feedback/                 # ⏸ [17] 팀 결정 후 활성화
│   └── build.gradle.kts
├── module-analytics/                # ⏸ [17] 팀 결정 후 활성화
│   └── build.gradle.kts
└── app/src/main/resources/
    ├── application.yml              # 공통 설정
    ├── application-dev.yml          # 로컬 개발 설정
    └── application-prod.yml         # 프로덕션 (SSM Parameter Store 주입)
```

---

## Task 1: Spring Boot 프로젝트 생성

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- Create: `app/build.gradle.kts`, `app/src/main/kotlin/com/atomiccv/AtomicCvApplication.kt`

### 선행 결정 확인

- [ ] `doc/discussion.md` [17] 확인 — `module-feedback` / `module-analytics` 분리 여부
  - 팀 미결 시: 모듈 디렉토리만 생성, `settings.gradle.kts` 포함 주석 처리로 진행

### Step 1: gradle.properties 작성

```properties
# gradle.properties
kotlinVersion=1.9.25
javaVersion=21
springBootVersion=3.4.5
springDependencyManagementVersion=1.1.7
```

### Step 2: settings.gradle.kts 작성

```kotlin
rootProject.name = "atomic-cv"

include(
    ":app",
    ":module-shared",
    ":module-auth",
    ":module-resume",
    // ":module-feedback",   // ⏸ [17] 팀 결정 후 주석 해제
    // ":module-analytics",  // ⏸ [17] 팀 결정 후 주석 해제
)
```

### Step 3: 루트 build.gradle.kts 작성

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

allprojects {
    group = "com.atomiccv"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    ktlint {
        version.set("1.5.0")
        android.set(false)
        outputToConsole.set(true)
        filter {
            exclude("**/generated/**")
        }
    }

    detekt {
        config.setFrom(rootProject.files("detekt.yml"))
        buildUponDefaultConfig = true
    }
}
```

### Step 4: app/build.gradle.kts 작성

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":module-shared"))
    implementation(project(":module-auth"))
    implementation(project(":module-resume"))
    // implementation(project(":module-feedback"))  // ⏸ [17]
    // implementation(project(":module-analytics"))  // ⏸ [17]

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
```

### Step 5: AtomicCvApplication.kt 작성

```kotlin
package com.atomiccv

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AtomicCvApplication

fun main(args: Array<String>) {
    runApplication<AtomicCvApplication>(*args)
}
```

### Step 6: 빌드 검증

```bash
./gradlew :app:build -x test
```

Expected: `BUILD SUCCESSFUL`

---

## Task 2: 멀티모듈 구조 설정

**Files:**
- Create: 각 모듈 `build.gradle.kts`
- Create: 각 모듈 패키지 디렉토리 (빈 `.gitkeep` 포함)

### Step 1: module-shared/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
```

### Step 2: module-shared 패키지 구조 생성

```
module-shared/src/main/kotlin/com/atomiccv/shared/
├── common/
│   ├── response/       # ApiResponse<T>
│   ├── exception/      # BusinessException, ErrorCode
│   └── util/           # 공통 유틸리티
```

```bash
mkdir -p module-shared/src/main/kotlin/com/atomiccv/shared/common/{response,exception,util}
mkdir -p module-shared/src/test/kotlin/com/atomiccv/shared
touch module-shared/src/main/kotlin/com/atomiccv/shared/common/response/.gitkeep
touch module-shared/src/main/kotlin/com/atomiccv/shared/common/exception/.gitkeep
touch module-shared/src/main/kotlin/com/atomiccv/shared/common/util/.gitkeep
```

### Step 3: module-auth/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
    }
}

dependencies {
    implementation(project(":module-shared"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("com.mysql:mysql-connector-j")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}
```

### Step 4: module-auth 패키지 구조 생성

```
module-auth/src/main/kotlin/com/atomiccv/auth/
├── domain/
│   ├── model/          # User, SocialAccount (순수 Kotlin)
│   └── repository/     # UserRepository (인터페이스)
├── application/
│   ├── usecase/        # LoginUseCase, LogoutUseCase, ...
│   ├── port/           # 모듈 간 통신 Port 인터페이스
│   └── event/          # 도메인 이벤트
├── infrastructure/
│   ├── persistence/    # JPA Entity, UserRepositoryImpl
│   └── client/         # GoogleOAuthClient, KakaoOAuthClient
└── interfaces/
    └── rest/           # AuthController, Request/Response DTO
```

```bash
mkdir -p module-auth/src/main/kotlin/com/atomiccv/auth/{domain/{model,repository},application/{usecase,port,event},infrastructure/{persistence,client},interfaces/rest}
mkdir -p module-auth/src/test/kotlin/com/atomiccv/auth/{domain,application,interfaces}
find module-auth/src -type d -empty -exec touch {}/.gitkeep \;
```

### Step 5: module-resume/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
    }
}

dependencies {
    implementation(project(":module-shared"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.mysql:mysql-connector-j")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

### Step 6: module-resume 패키지 구조 생성

```
module-resume/src/main/kotlin/com/atomiccv/resume/
├── domain/
│   ├── model/          # Resume, Block, ResumeVersion (순수 Kotlin)
│   └── repository/     # ResumeRepository, BlockRepository
├── application/
│   ├── usecase/        # CreateResumeUseCase, PublishResumeUseCase, ...
│   ├── port/           # BlockQueryPort (feedback·analytics에서 사용)
│   └── event/          # ResumePublishedEvent
├── infrastructure/
│   └── persistence/    # JPA Entity, RepositoryImpl
└── interfaces/
    └── rest/           # ResumeController, BlockController
```

```bash
mkdir -p module-resume/src/main/kotlin/com/atomiccv/resume/{domain/{model,repository},application/{usecase,port,event},infrastructure/persistence,interfaces/rest}
mkdir -p module-resume/src/test/kotlin/com/atomiccv/resume/{domain,application,interfaces}
find module-resume/src -type d -empty -exec touch {}/.gitkeep \;
```

### Step 7: 모듈 빌드 전체 검증

```bash
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL` (모든 모듈)

---

## Task 3: ktlint + detekt 설정

**Files:**
- Create: `.editorconfig`
- Create: `detekt.yml`

### Step 1: .editorconfig 작성

```ini
[*.{kt,kts}]
indent_size = 4
continuation_indent_size = 4
max_line_length = 120
insert_final_newline = true
trim_trailing_whitespace = true

# ktlint 규칙
ktlint_standard_no-wildcard-imports = enabled
ktlint_standard_trailing-comma-on-call-site = disabled
ktlint_standard_trailing-comma-on-declaration-site = disabled
```

### Step 2: detekt.yml 작성

```yaml
build:
  maxIssues: 0

complexity:
  LongMethod:
    threshold: 40
  LongParameterList:
    functionThreshold: 6
    constructorThreshold: 8
  CyclomaticComplexMethod:
    threshold: 15

naming:
  FunctionNaming:
    active: true
    functionPattern: '[a-z][a-zA-Z0-9]*'
    excludes: ['**/test/**', '**/*Test.kt', '**/*Spec.kt']

style:
  MaxLineLength:
    maxLineLength: 120
  WildcardImport:
    active: true
  UnusedImports:
    active: true

exceptions:
  TooGenericExceptionCaught:
    active: true

comments:
  UndocumentedPublicClass:
    active: false
  UndocumentedPublicFunction:
    active: false
```

### Step 3: ktlint 검사 실행

```bash
./gradlew ktlintCheck
```

Expected: `BUILD SUCCESSFUL` (경고 없음)

### Step 4: detekt 검사 실행

```bash
./gradlew detekt
```

Expected: `BUILD SUCCESSFUL` (이슈 없음)

---

## Task 4: pre-commit hook 설정

**Files:**
- Create: `.githooks/pre-commit`

### Step 1: .githooks 디렉토리 생성 및 pre-commit 스크립트 작성

```bash
mkdir -p .githooks
```

`.githooks/pre-commit`:

```bash
#!/bin/bash
set -e

echo "▶ ktlint 검사 실행..."
./gradlew ktlintCheck --daemon

if [ $? -ne 0 ]; then
  echo "❌ ktlint 검사 실패 — 코드 스타일 오류를 수정 후 다시 커밋하세요."
  echo "   자동 수정: ./gradlew ktlintFormat"
  exit 1
fi

echo "✅ ktlint 검사 통과"
```

### Step 2: 실행 권한 부여

```bash
chmod +x .githooks/pre-commit
```

### Step 3: git hooks 경로 설정

```bash
git config core.hooksPath .githooks
```

### Step 4: hook 동작 검증

```bash
# 일부러 스타일 오류 있는 파일로 커밋 시도 후 hook 차단 확인
git commit --allow-empty -m "test: pre-commit hook 동작 확인"
```

Expected: ktlint 통과 시 커밋 성공, 실패 시 `❌ ktlint 검사 실패` 출력 후 중단

---

## Task 5: application.yml 프로파일 분리

**Files:**
- Create: `app/src/main/resources/application.yml`
- Create: `app/src/main/resources/application-dev.yml`
- Create: `app/src/main/resources/application-prod.yml`

### Step 1: application.yml (공통 설정)

```yaml
spring:
  application:
    name: atomic-cv

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
    open-in-view: false

  jackson:
    default-property-inclusion: non_null
    time-zone: Asia/Seoul

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

### Step 2: application-dev.yml (로컬 개발 환경)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update       # 로컬 개발 시 스키마 자동 반영
    show-sql: true

  data:
    redis:
      host: localhost
      port: 6379

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

jwt:
  secret: ${JWT_SECRET}
  access-expiry: ${JWT_ACCESS_EXPIRY:3600000}
  refresh-expiry: ${JWT_REFRESH_EXPIRY:604800000}

logging:
  level:
    com.atomiccv: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Step 3: application-prod.yml (프로덕션 — SSM Parameter Store 주입)

```yaml
# 프로덕션 환경변수는 AWS SSM Parameter Store에서 deploy.sh가 주입
# 경로: /atomiccv/prod/*

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate      # 프로덕션은 스키마 자동 변경 금지
    show-sql: false

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      ssl:
        enabled: true         # ElastiCache TLS 연결

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

jwt:
  secret: ${JWT_SECRET}
  access-expiry: ${JWT_ACCESS_EXPIRY}
  refresh-expiry: ${JWT_REFRESH_EXPIRY}

logging:
  level:
    com.atomiccv: INFO
    root: WARN
```

### Step 4: 로컬 환경변수 설정 방법 안내

로컬 개발 시 `.env` 파일을 직접 사용하지 않는다 (AWS RDB 직접 연결).  
IntelliJ Run Configuration 또는 쉘 환경변수로 아래 값을 설정한다:

```bash
# 로컬 개발용 환경변수 (커밋 금지)
export DB_URL=jdbc:mysql://<RDS_ENDPOINT>:3306/atomiccv_dev
export DB_USERNAME=<username>
export DB_PASSWORD=<password>
export JWT_SECRET=<32자_이상_랜덤_문자열>
export SMTP_USERNAME=<gmail_address>
export SMTP_PASSWORD=<gmail_app_password>
```

### Step 5: dev 프로파일로 앱 기동 확인

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew :app:bootRun
```

Expected:
```
Started AtomicCvApplication in X.XXX seconds
```

Health Check:
```bash
curl http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"}`

---

## 완료 기준 체크리스트

- [x] `./gradlew build -x test` — BUILD SUCCESSFUL
- [x] `./gradlew ktlintCheck` — BUILD SUCCESSFUL
- [x] `./gradlew detekt` — BUILD SUCCESSFUL
- [x] pre-commit hook이 ktlint 실패 시 커밋을 차단함 (실제 커밋 시 동작 확인)
- [x] `SPRING_PROFILES_ACTIVE=dev ./gradlew :app:bootRun` — 앱 정상 기동 (DB 연결 성공, 1.88초 기동 확인)
- [x] `curl http://localhost:8080/actuator/health` — `{"status":"UP"}`
- [x] `doc/TASKS.md` 1-2-1 ~ 1-2-5 항목 상태 🟢 업데이트

---

## 커밋 구성

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties
git commit -m "chore: Spring Boot 멀티모듈 프로젝트 초기 구조 생성"

git add module-shared/ module-auth/ module-resume/ app/
git commit -m "chore: 멀티모듈 패키지 구조 및 의존성 설정"

git add .editorconfig detekt.yml
git commit -m "chore: ktlint, detekt 코드 스타일 도구 설정"

git add .githooks/
git commit -m "chore: pre-commit hook 설정 (ktlint 검사)"

git add app/src/main/resources/
git commit -m "chore: application.yml dev/prod 프로파일 분리"
```
