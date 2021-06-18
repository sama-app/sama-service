package com.sama.events

import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.EventBus
import com.sama.events.guava.GuavaEventDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class EventConfiguration {

    @Bean
    fun guavaEventBus(): EventBus {
        return AsyncEventBus("sama-async", Executors.newCachedThreadPool());
    }

    @Bean
    fun eventDispatcher(guavaEventBus: EventBus): EventDispatcher {
        return GuavaEventDispatcher(guavaEventBus)
    }

    @Bean
    fun eventPublisher(eventDispatcher: EventDispatcher): EventPublisher {
        return TransactionalEventPublisher(eventDispatcher)
    }
}