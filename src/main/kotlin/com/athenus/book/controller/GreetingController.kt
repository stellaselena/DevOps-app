package com.athenus.book.controller

import com.codahale.metrics.MetricRegistry
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class GreetingController {

    @Autowired
    private lateinit var metrics : MetricRegistry

    @ApiOperation("Greeting")
    @GetMapping(path = ["/"])
    fun greet(): String {

        metrics.counter("greeting").inc()

        return "Book REST service. Api documentation can be accessed from: /swagger-ui.html."
    }
}