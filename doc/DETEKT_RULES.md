# Detekt / ktlint 위반 패턴

> 코드 작성 시 아래 규칙을 **사전에** 준수한다. CI에서 매번 실패하는 원인이다.

---

## ThrowsCount (함수당 throw 최대 2개)

```kotlin
// ❌ throw 3개 → Detekt 위반
fun withdraw(command: WithdrawCommand) {
    val social = repo.find() ?: throw ...  // 1
    if (!social.isActive) throw ...        // 2
    val user = repo.findById() ?: throw ... // 3 ← 위반
}

// ✅ 3번째 throw를 private 함수로 추출
fun withdraw(command: WithdrawCommand) {
    val social = repo.find() ?: throw ...
    if (!social.isActive) throw ...
    if (noActiveSocial) deactivateUser(command)  // throw는 내부로
}
private fun deactivateUser(command: WithdrawCommand) {
    val user = repo.findById() ?: throw ...
}
```

---

## ReturnCount (함수당 return 최대 2개)

```kotlin
// ❌ return 3개
fun validate(file: MultipartFile): String? {
    if (type !in allowed) return "타입 오류"
    if (size > max) return "크기 초과"
    return null  // 3개
}

// ✅ when 표현식으로 단일 return
fun validate(file: MultipartFile): String? =
    when {
        file.contentType !in allowed -> "타입 오류"
        file.size > max -> "크기 초과"
        else -> null
    }
```

---

## TooGenericExceptionCaught

```kotlin
// ❌
} catch (e: Exception) { ... }

// ✅ 구체적 타입 사용
} catch (e: RestClientException) { ... }

// ✅ 불가피하게 포괄 catch가 필요한 경우 (외부 포트 예외 불명확 등)
@Suppress("TooGenericExceptionCaught")
} catch (e: Exception) { ... }
```

---

## UseCheckOrError

```kotlin
// ❌
throw IllegalStateException("메시지")

// ✅ Kotlin 표준 shorthand 사용
error("메시지")          // IllegalStateException
check(condition) { "메시지" }  // IllegalStateException (조건부)
```

---

## LongMethod / LongParameterList

- 함수가 25줄을 넘으면 private 함수로 분리한다.
- 파라미터가 6개를 넘으면 data class로 묶는다.

---

## 커밋 전 로컬 검사 (Java 21 필요)

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-{name}:detekt
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :module-{name}:ktlintFormat
```

> **중요:** macOS 기본 JDK가 26이면 Gradle이 실패한다.
> 모든 Gradle 명령 앞에 `JAVA_HOME=$(/usr/libexec/java_home -v 21)` 을 붙인다.
