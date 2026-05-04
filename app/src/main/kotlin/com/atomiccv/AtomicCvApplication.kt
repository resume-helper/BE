package com.atomiccv

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class AtomicCvApplication

fun main(args: Array<String>) {
    runApplication<AtomicCvApplication>(*args)
}
