package com.sama.events

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive
import org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization

class TransactionalEventPublisher(private val eventDispatcher: EventDispatcher) : EventPublisher {

    override fun <T> publish(event: T) {
        check(isSynchronizationActive()) { "Must be running inside a transaction" }
        registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                eventDispatcher.dispatch(event)
            }
        })
    }
}