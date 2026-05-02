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

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        val lines = envFile.readLines()
        val entries = lines.filter { line -> line.isNotBlank() && !line.startsWith("#") }
        entries.forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                environment(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
            }
        }
    }
}
