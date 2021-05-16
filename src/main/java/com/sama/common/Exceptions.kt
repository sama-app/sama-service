package com.sama.common

import kotlin.reflect.KClass

class NotFoundException(clazz: KClass<*>, field: String, value: Any) :
    RuntimeException("${clazz.simpleName} by $field#$value not found") {

    constructor(clazz: KClass<*>, id: Any) : this(clazz, "Id", id)
}
