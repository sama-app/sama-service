package com.sama.common

import java.util.*

fun <T : Any> Optional<T>.toNullable(): T? = this.orElse(null)