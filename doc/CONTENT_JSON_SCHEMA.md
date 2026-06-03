# CONTENT_JSON_SCHEMA.md — 블록 타입별 contentJson 스키마

> 블록 생성/수정 시 `contentJson` 필드 검증 기준.
> `필수` = 항상 필요, `조건 필수` = 특정 조건 하에 필요, `선택` = 선택 입력.

---

## BASIC_INFO

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 이름 | `name` | 필수 | `string` | |
| 이메일 | `email` | 필수 | `string` | 이메일 형식 (@ 포함) |
| 전화번호 | `phoneNumber` | 필수 | `string` | `010-0000-0000` 형식 |
| 프로필 이미지 | `profileImage` | 선택 | `string` | S3 URL |

---

## SUMMARY

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 기본소개 제목 | `title` | 필수 | `string` | |
| 상세내용 | `content` | 필수 | `string` | |

---

## INTRODUCTION

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 자기소개서 제목 | `title` | 필수 | `string` | |
| 상세내용 | `content` | 필수 | `string` | |

---

## CAREER

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 회사명 | `companyName` | 필수 | `string` | |
| 근무부서 | `department` | 필수 | `string` | |
| 직무 | `jobTitle` | 필수 | `string` | |
| 직급/직책 | `position` | 필수 | `string` | |
| 재직형태 | `employmentType` | 필수 | `enum` | `FULL_TIME` \| `CONTRACT` \| `INTERN` \| `FREELANCER` |
| 입사년월 | `startDate` | 필수 | `string` | `YYYY.MM` |
| 퇴사년월 | `endDate` | 필수 | `string` | `YYYY.MM` |
| 주요성과 | `achievements` | 필수 | `string` | |

---

## PROJECT

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 프로젝트명 | `projectName` | 필수 | `string` | |
| 시작일 | `startDate` | 필수 | `string` | `YYYY.MM.DD` |
| 종료일 | `endDate` | 필수 | `string` | `YYYY.MM.DD` |
| 기여도 | `contribution` | 선택 | `number` | |
| 사용 기술 스택 | `techStacks` | 선택 | `string[]` | |
| 문제 해결 과정 | `problemSolving` | 선택 | `string` | |
| 링크 | `link` | 선택 | `string` | |

---

## SKILL

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 기술스택명 | `skillName` | 필수 | `string` | |
| 숙련도 | `proficiency` | 선택 | `enum` | `LOW` \| `MEDIUM` \| `HIGH` |
| 활용범위 | `usageScope` | 필수 | `string` | |

---

## EDUCATION

공통 필드:

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 학력구분 | `educationLevel` | 필수 | `enum` | `COLLEGE_OR_ABOVE` \| `HIGH_SCHOOL` \| `OTHER` |

### COLLEGE_OR_ABOVE (대학·대학원 이상 졸업)

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 대학구분 | `universityType` | 필수 | `enum` | `BACHELORS` \| `ASSOCIATE` \| `MASTERS` \| `DOCTORATE` |
| 학교명 | `schoolName` | 필수 | `string` | |
| 전공 | `major` | 필수 | `string` | |
| 졸업여부 | `graduationStatus` | 필수 | `enum` | `GRADUATED` \| `ENROLLED` \| `ON_LEAVE` \| `EXPECTED_GRADUATION` \| `DROPPED_OUT` \| `COMPLETED` |
| 입학년월 | `startDate` | 필수 | `string` | `YYYY.MM` |
| 졸업년월 | `endDate` | 필수 | `string` | `YYYY.MM` |
| 추가 전공 | `doubleMajor` | 선택 | `string` | |
| 전공구분 | `majorType` | 선택 | `enum` | `DOUBLE_MAJOR` \| `MINOR` |
| 학점 | `gpa` | 선택 | `number` | |
| 기준학점 | `gpaMax` | 선택 | `number` | `4.5` \| `4.3` \| `4.0` |
| 편입여부 | `isTransfer` | 선택 | `boolean` | |

### HIGH_SCHOOL (고등학교 졸업)

`isGed = false` (기본):

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 학교명 | `schoolName` | 필수 | `string` | |
| 졸업여부 | `graduationStatus` | 필수 | `enum` | `GRADUATED` \| `ENROLLED` \| `EXPECTED_GRADUATION` \| `DROPPED_OUT` |
| 대입 검정고시 여부 | `isGed` | 선택 | `boolean` | 기본값 `false` |
| 전공계열 | `majorField` | 선택 | `enum` | `GENERAL` \| `SPECIALIZED_SCIENCE_FOREIGN` \| `VOCATIONAL_MEISTER` |
| 전공 | `major` | 선택 | `string` | |
| 입학년월 | `startDate` | 선택 | `string` | `YYYY.MM` |
| 졸업년월 | `endDate` | 선택 | `string` | `YYYY.MM` |

`isGed = true` (대입 검정고시) — 위 필드 불필요, 아래만 허용:

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 대입 검정고시 여부 | `isGed` | 필수 | `boolean` | `true` |
| 합격년월 | `endDate` | 선택 | `string` | `YYYY.MM` |

### OTHER (기타 학력)

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 학력 유형 | `educationType` | 필수 | `string` | 예) 학점인증제, 평생교육원 |
| 기관명 | `institutionName` | 필수 | `string` | |
| 이수 여부 | `completed` | 필수 | `enum` | `COMPLETED` \| `IN_PROGRESS` \| `DISCONTINUED` |
| 시작년월 | `startDate` | 선택 | `string` | `YYYY.MM` |
| 종료년월 | `endDate` | 선택 | `string` | `YYYY.MM` |

---

## CERTIFICATE

공통 필드:

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 수상・자격 구분 | `certificateType` | 필수 | `enum` | `AWARD_CONTEST` \| `CERTIFICATE` \| `LANGUAGE` \| `OTHER` |
| 명칭 | `name` | 필수 | `string` | |
| 수상일/취득일 | `issuedDate` | 필수 | `string` | `YYYY.MM` |
| 발급기관 | `issuer` | 필수 | `string` | |

### LANGUAGE (어학) 추가 필드

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 점수/등급 | `scoreGrade` | 선택 | `string` | |

### OTHER (기타) 추가 필드

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 구분 | `category` | 필수 | `string` | |

---

## ACTIVITY

| 한글명 | 필드명 | 필수여부 | 타입 | 제약조건 |
|--------|--------|----------|------|----------|
| 활동 구분 | `activityType` | 필수 | `enum` | `SCHOOL_ACTIVITY` \| `EXTERNAL_ACTIVITY` \| `INTERN` \| `EDUCATION_TRAINING` \| `OTHER` |
| 활동명 | `activityName` | 필수 | `string` | |
| 기관/장소명 | `organizationName` | 필수 | `string` | |
| 시작년월 | `startDate` | 필수 | `string` | `YYYY.MM` |
| 종료년월 | `endDate` | 필수 | `string` | `YYYY.MM` |
| 경험/활동 내역 | `description` | 필수 | `string` | |

---

## 검증 구현 시 유의사항

- **날짜 형식**: `YYYY.MM` vs `YYYY.MM.DD` 블록마다 다름 — PROJECT만 일(day)까지 검증 필요
- **EDUCATION 분기**: `educationLevel` 값 기준으로 검증 로직 3분기 필요 (`COLLEGE_OR_ABOVE` / `HIGH_SCHOOL` / `OTHER`)
- **HIGH_SCHOOL 내부 분기**: `isGed = true`이면 `schoolName`, `graduationStatus`, `majorField`, `major`, `startDate` 검증 제외 — `endDate`(합격년월)만 허용
- **HIGH_SCHOOL graduationStatus 제한**: `ON_LEAVE`, `COMPLETED`는 고등학교에 없음 — 대학 전용 값
- **CERTIFICATE 분기**: `certificateType = LANGUAGE`이면 `scoreGrade` 허용, `OTHER`이면 `category` 필수
- **enum 값**: 코드에서 별도 enum 클래스로 관리 권장
- **`majorField` 필드명**: 노션 원본 `major_field`는 Kotlin/JSON 직렬화 시 camelCase(`majorField`)로 통일
