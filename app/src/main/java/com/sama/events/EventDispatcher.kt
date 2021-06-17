package com.sama.events

interface EventDispatcher {
    fun <T> dispatch(event: T)
}