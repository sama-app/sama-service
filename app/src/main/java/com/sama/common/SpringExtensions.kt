package com.sama.common

import org.springframework.data.repository.CrudRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

inline fun <reified T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T = findById(id)
    .orElseThrow { NotFoundException(T::class, id) }

fun checkAccess(value: Boolean) {
    checkAccess(value) { "Access Denied" }
}

/**
 * Throws [AccessDeniedException] if the value is false.
 */
inline fun checkAccess(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        val message = lazyMessage()
        throw AccessDeniedException(message.toString())
    }
}

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