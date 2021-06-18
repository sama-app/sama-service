package com.sama.events.guava

import com.google.common.eventbus.EventBus
import com.sama.events.EventConsumer
import org.springframework.stereotype.Component

@Component
class GuavaEventListenerRegistrations(
    private val eventBus: EventBus,
    eventConsumers: List<EventConsumer>
) {

    init{
        eventConsumers.forEach { eventBus.register(it) }
    }
}