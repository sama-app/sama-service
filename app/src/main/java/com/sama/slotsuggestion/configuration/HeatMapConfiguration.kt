package com.sama.slotsuggestion.configuration

import com.sama.slotsuggestion.domain.v1.WeightContext
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.slotsuggestion.heat-map")
class HeatMapConfiguration(
    val intervalMinutes: Long,
    val historicalDays: Long,
    val futureDays: Long,
)

@Configuration
class WeightConfiguration {

    @Bean
    fun weightContext(heatMapConfiguration: HeatMapConfiguration): WeightContext {
        return WeightContext(heatMapConfiguration.intervalMinutes.toInt(), heatMapConfiguration.futureDays.toInt())
    }
}