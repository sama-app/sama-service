package com.sama.slotsuggestion.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "sama.slotsuggestion.heat-map")
class HeatMapConfiguration(
    val historicalDays: Long,
    val futureDays: Long,
)