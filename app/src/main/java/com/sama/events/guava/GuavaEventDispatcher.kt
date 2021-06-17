package com.sama.events.guava

import com.google.common.eventbus.EventBus
import com.sama.events.EventDispatcher

class GuavaEventDispatcher(private val eventBus: EventBus) : EventDispatcher {

    override fun <T> dispatch(event: T) {
        eventBus.post(event)
    }
}