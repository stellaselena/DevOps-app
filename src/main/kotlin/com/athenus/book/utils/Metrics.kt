package com.athenus.book.utils

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricFilter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Configuration
class GraphiteMetricsConfig {

    @Bean
    fun getRegistry(): MetricRegistry {
        return MetricRegistry()
    }

    @Bean
    fun getReporter(registry: MetricRegistry): ConsoleReporter {
        val reporter = ConsoleReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build()
        reporter.start(1, TimeUnit.SECONDS)
        return reporter
    }
}