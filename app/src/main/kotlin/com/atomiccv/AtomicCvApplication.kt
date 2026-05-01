package com.atomiccv

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AtomicCvApplication

fun main(args: Array<String>) {
    runApplication<AtomicCvApplication>(*args)
}
