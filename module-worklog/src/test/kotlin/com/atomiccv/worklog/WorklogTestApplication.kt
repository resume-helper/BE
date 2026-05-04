package com.atomiccv.worklog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.test.context.ActiveProfiles

@SpringBootApplication
@EnableJpaAuditing
@ActiveProfiles("test")
class WorklogTestApplication
