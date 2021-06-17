package com.sama.events

interface EventPublisher {
    fun <T> publish(event: T)
}