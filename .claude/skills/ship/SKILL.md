---
name: ship
description: Use when the user wants to commit current changes, run tests, push to remote, and create a GitHub PR. Trigger with /ship or when the user says "커밋하고 푸시해", "PR 올려줘", "배포해줘".
trigger: /ship
---

# /ship — 테스트 → 커밋 → 푸시 → PR

`$ARGUMENTS`가 있으면 PR 제목으로 사용합니다. 없으면 변경사항을 분석해 자동 생성합니다.

---

## 1단계 — 변경사항 파악 (병렬 실행)

아래 3개 명령을 **동시에** 실행합니다.

```bash
git status
git diff
git log --oneline -5
```

---

## 2단계 — 스테이징 (민감 파일 제외)

아래 파일은 **절대 스테이징하지 않습니다**:
- `.env`, `*.local`
- `build/`, `*.class`
- `.claude/`
- `*.pem`, `*secret*`, `*credential*`

변경된 소스 파일만 선택적으로 `git add`합니다.  
`git add -A` 또는 `git add .` 사용 금지 — 파일명을 명시해서 스테이징합니다.

---

## 3단계 — 테스트 실행 (커밋 전 필수)

```bash
./gradlew test --continue 2>&1
```

`./gradlew`가 없으면 `gradle test --continue`로 대체합니다.

### 테스트 결과 수집

테스트 완료 후 XML 리포트를 파싱합니다:

```bash
find . -path "*/build/test-results/test/*.xml" -not -path "*/.gradle/*"
```

각 XML `<testsuite>` 태그에서 추출: `name`, `tests`, `failures`, `errors`, `skipped`  
실패 케이스: `<testcase>` 내 `<failure>` 또는 `<error>` 태그 내용

### 결과에 따른 처리 규칙

| 상황 | 처리 |
|------|------|
| 빌드 오류 (컴파일 실패) | **즉시 중단** — 오류 내용 보고, 커밋하지 않음 |
| 테스트 케이스 실패 | 실패 목록을 보여주고 **사용자에게 계속 진행 여부 확인** |
| 테스트 없음 | 경고 출력 후 계속 진행 |
| 전체 통과 | 4단계로 진행 |

---

## 4단계 — 커밋

`CLAUDE.md`의 커밋 컨벤션에 맞게 HEREDOC 방식으로 커밋합니다:

```bash
git commit -m "$(cat <<'EOF'
feat(module): 내용

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

커밋 타입: `feat` / `fix` / `refactor` / `test` / `docs` / `ci` / `chore`

---

## 5단계 — Push

```bash
# 현재 브랜치 확인
git branch --show-current

# push
git push -u origin HEAD
```

`main` 브랜치에서 직접 push 시도 시 → 사용자에게 확인 후 진행합니다.

---

## 6단계 — PR 생성

```bash
gh pr create --base main --title "제목" --body "$(cat <<'EOF'
본문
EOF
)"
```

### PR 본문 형식

```markdown
## Summary
- 변경사항 bullet point 요약

## Test Results
| 모듈 | 전체 | 통과 | 실패 | 스킵 |
|------|------|------|------|------|
| module-xxx | n | n | n | n |

## Test Plan

<!-- 테스트가 실행된 경우 -->
- [x] ModuleName: TestClassName — PASSED (n개)
- [ ] ModuleName: TestClassName — FAILED (실패케이스명)

<!-- 테스트가 없는 경우: 변경 파일 분석 후 수동 체크박스 생성 -->
- [ ] {UseCase명} 정상 동작 확인
- [ ] {엔드포인트} 응답 형식 확인
- [ ] 애플리케이션 기동 확인

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

### Test Plan 자동 생성 규칙

**테스트 결과 있음:**
- 통과 → `- [x] {모듈}: {클래스명} — PASSED ({n}개)`
- 실패 → `- [ ] {모듈}: {클래스명} — FAILED ({실패케이스명})`
- 스킵 → `- [ ] {클래스명} — SKIPPED`

**테스트 없음 — 변경 파일 분석:**
- UseCase 변경 → `- [ ] {UseCase명} 정상 동작 확인`
- Controller/API 변경 → `- [ ] {엔드포인트} 응답 형식 확인`
- DB/Migration 변경 → `- [ ] 스키마 마이그레이션 확인`
- 설정 변경 → `- [ ] 애플리케이션 기동 확인`

---

## 주의사항

- `.env`, `*.local`, `build/`, `*.class`, `.claude/` 파일은 절대 커밋하지 않습니다
- PR base 브랜치는 `main`입니다
- XML 리포트가 없으면 `./gradlew test` 출력 텍스트에서 결과를 파싱합니다
- `main` 브랜치 직접 push는 사용자 확인 후 진행합니다
