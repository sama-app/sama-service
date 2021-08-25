package com.sama.common

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager


/**
 * Executes the given runnable after a database transaction commits. If not in a transaction,
 * executes it immediately.
 */
fun afterCommit(runnable: Runnable) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        runnable.run()
        return
    }

    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
        override fun afterCommit() {
            runnable.run()
        }
    })
}