rootProject.name = "atomic-cv"

include(
    ":app",
    ":module-shared",
    ":module-auth",
    ":module-resume",
    ":module-worklog",
    // ":module-feedback",   // ⏸ 팀 결정 후 주석 해제
    // ":module-analytics",  // ⏸ 팀 결정 후 주석 해제
)
