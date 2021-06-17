package com.sama.events

import com.google.common.eventbus.AsyncEventBus
import com.sama.calendar.application.BlockEventConsumer
import com.sama.events.guava.GuavaEventDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class EventConfiguration {

    @Bean
    fun eventDispatcher(listener: BlockEventConsumer): EventDispatcher {
        val eventBus = AsyncEventBus("sama-async", Executors.newCachedThreadPool())
        eventBus.register(listener)
        return GuavaEventDispatcher(eventBus);
    }

    @Bean
    fun eventPublisher(eventDispatcher: EventDispatcher): EventPublisher {
        return TransactionalEventPublisher(eventDispatcher)
    }
}