package com.athenus.book

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = arrayOf("com.athenus.book"))
class BookApplication

fun main(args: Array<String>) {
    SpringApplication.run(BookApplication::class.java, *args)
}