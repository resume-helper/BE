plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // implementation(project(":module-shared"))  // ⏸ Task 2에서 모듈 생성 후 주석 해제
    // implementation(project(":module-auth"))    // ⏸ Task 2에서 모듈 생성 후 주석 해제
    // implementation(project(":module-resume"))  // ⏸ Task 2에서 모듈 생성 후 주석 해제
    // implementation(project(":module-feedback"))  // ⏸
    // implementation(project(":module-analytics"))  // ⏸

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
