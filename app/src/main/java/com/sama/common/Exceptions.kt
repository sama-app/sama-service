package com.sama.common

import kotlin.reflect.KClass

class NotFoundException(clazz: KClass<*>, field: String, value: Any?) :
    RuntimeException("${clazz.simpleName} by $field#$value not found") {
    constructor(clazz: KClass<*>, id: Any?) : this(clazz, "Id", id)
}

interface HasReason {
    val reason: String
}

open class DomainValidationException(message: String) : RuntimeException(message)

open class DomainIntegrityException(override val reason: String, message: String) :
    RuntimeException(message), HasReason

open class DomainEntityStatusException(override val reason: String, message: String) :
    RuntimeException(message), HasReason

open class DomainInvalidActionException(override val reason: String, message: String) :
    RuntimeException(message), HasReason
