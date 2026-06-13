# 이미지/파일 업로드 기능을 공동 모듈로 추출 — 설계

작성일: 2026-06-13
대상 모듈: `module-resume` → `module-shared`

---

## 1. 배경

현재 S3 presigned URL 기반 이미지/파일 업로드 기능은 `module-resume` 안에 묶여 있어 Block·Profile 등 다른 도메인에서 재사용할 수 없다. 향후 Block 콘텐츠 안 이미지 첨부, Profile 이미지 업로드 등 다도메인 사용 요구가 예상되므로, **공동 모듈 `module-shared` 로 추출하여 재사용 가능하게** 만든다.

이번 범위는 **위치 이동(리팩토링) 만** 이다. 새 엔드포인트, 새 비즈니스 로직, key prefix 일반화 같은 확장은 후속 작업으로 분리한다.

---

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|------|------|------|
| 추상화 레벨 | **재사용만** — 현재 UseCase 를 그대로 끌어올림 | 가장 작은 변경. 도메인별 정책 분리 같은 추상화는 실제 요구 발생 시 도입 (YAGNI) |
| 공동 모듈 위치 | `module-shared` 안 sub-package | 새 모듈 생성 없이 단순화. S3 SDK 가 module-auth/worklog 클래스패스에도 들어가는 오염은 수용 |
| 엔드포인트 노출 | 현 `POST /api/resumes/upload-url` 유지. **새 endpoint 추가 안 함** | Block/Profile 사용은 별도 작업. 이번에는 이동만 |
| key prefix | `resumes/{userId}/{UUID}/{fileName}` 그대로 | 일반화는 Block 사용 endpoint 추가 시점에 별도 처리 |

---

## 3. 이동 대상

| 항목 | from | to |
|------|------|----|
| `S3Port` (interface) | `com.atomiccv.resume.infrastructure.s3` (in module-resume) | `com.atomiccv.shared.infrastructure.storage` (in module-shared) |
| `S3Adapter` | 동일 | 동일 |
| `GenerateUploadUrlUseCase` | `com.atomiccv.resume.application.usecase` | `com.atomiccv.shared.application.usecase` |
| `GenerateUploadUrlCommand` | 동일 | 동일 |
| `UploadUrlResult` | 동일 | 동일 |
| `S3Presigner` + `S3Adapter` Bean 정의 | `ResumeModuleConfiguration` (in module-resume) | 새 `SharedStorageConfiguration` (`@Configuration` in module-shared, `com.atomiccv.shared.infrastructure.storage`) |
| AWS S3 SDK 의존성 (`software.amazon.awssdk:bom`, `s3`) | `module-resume/build.gradle.kts` | `module-shared/build.gradle.kts` |
| `GenerateUploadUrlUseCaseTest` | `module-resume/.../test/.../application/usecase` | `module-shared/.../test/.../application/usecase` |

---

## 4. ResumeController 변경

import 경로만 갱신.

```diff
- import com.atomiccv.resume.application.usecase.GenerateUploadUrlUseCase
+ import com.atomiccv.shared.application.usecase.GenerateUploadUrlUseCase
```

엔드포인트(`POST /api/resumes/upload-url`), Request/Response DTO, 동작 모두 그대로. 생성자 주입은 Spring DI 가 자동 해석.

`GetResumeUseCase.getPresignedDownloadUrl(pdfS3Key)` 도 동일하게 `S3Port` import 경로만 갱신.

---

## 5. 구성/설정

### 5.1 module-shared/build.gradle.kts

AWS SDK 의존성 추가.

```kotlin
dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.26.12"))
    implementation("software.amazon.awssdk:s3")
}
```

### 5.2 module-resume/build.gradle.kts

S3 관련 의존성 제거. `module-resume` 코드는 S3 SDK 타입을 직접 import 하지 않고 `S3Port` 인터페이스(String/Duration 만 노출)만 호출하므로 SDK 가 컴파일 클래스패스에 필요 없다. SDK 자체는 `module-shared` 가 `implementation` 으로 보유하면 런타임에 충분히 로딩된다.

### 5.3 SharedStorageConfiguration

`module-shared/src/main/kotlin/com/atomiccv/shared/infrastructure/storage/SharedStorageConfiguration.kt` 신규.

```kotlin
@Configuration
class SharedStorageConfiguration(
    @Value("\${cloud.aws.region.static}") private val region: String,
    @Value("\${resume.s3.bucket-name}") private val bucketName: String,
) {
    @Bean
    fun s3Presigner(): S3Presigner = S3Presigner.builder().region(Region.of(region)).build()

    @Bean
    fun s3Adapter(presigner: S3Presigner): S3Port = S3Adapter(presigner, bucketName)
}
```

`@ComponentScan` 이 `com.atomiccv` 루트를 스캔하므로 별도 import 불필요.

### 5.4 application yml

`cloud.aws.region.static`, `resume.s3.bucket-name` **그대로 둠**. 키 명은 후속 작업(일반화)에서 정리.

---

## 6. 테스트 전략

- `GenerateUploadUrlUseCaseTest` 를 module-shared 의 동일 패키지로 이동. 내용 변경 없음.
- 검증 명령:
  - `./gradlew :module-shared:ktlintCheck :module-shared:detekt :module-shared:test`
  - `./gradlew :module-resume:ktlintCheck :module-resume:detekt :module-resume:test`
  - 회귀 확인: 기존 `module-resume` 의 ResumeControllerTest 의 upload-url 케이스가 그대로 통과해야 한다.

---

## 7. 후속 작업 (이번 PR 미포함)

- `BlockController` 에 자기 `upload-url` 엔드포인트 추가 (공유 `GenerateUploadUrlUseCase` 주입).
- `GenerateUploadUrlCommand` 에 `prefix` 또는 `context` 파라미터 추가 → 도메인별 key prefix 분리.
- Profile 이미지 업로드 endpoint (필요 시).
- `resume.s3.bucket-name` 프로퍼티 키 일반화 (`storage.s3.bucket-name` 등).

---

## 8. 위험 요인

- S3 SDK 의존성이 module-auth, module-worklog 클래스패스에도 들어감. 빌드 사이즈 약간 증가. 실행 시 영향 없음.
- ResumeModuleConfiguration 의 Bean 제거 시 Spring 컨텍스트 로딩 실패 가능성 — `SharedStorageConfiguration` 이 정상 스캔되는지 컨텍스트 로딩 테스트로 확인 필요.
- `GenerateUploadUrlUseCase` 가 module-shared 의 `application.usecase` 패키지를 새로 만들게 됨. 기존 module-shared 는 application 레이어가 없는 인프라성 모듈이라, 도메인 영역 의식 측면에서 컨벤션 충돌 우려. 다만 본 작업 결정상 그대로 진행.

---

## 9. 완료 기준

- `module-resume` 안에 S3 관련 코드·의존성이 남아있지 않다.
- `module-shared:test` 와 `module-resume:test` 모두 통과한다.
- 기존 `POST /api/resumes/upload-url` 의 응답 동작·DTO·경로가 변경 전과 동일하다.
- 전체 ktlint + detekt 통과.
