package com.sama.api.config

import com.sama.users.domain.UserEntity
import io.swagger.v3.oas.annotations.Parameter

/**
 * Annotation to retrieve an ID an of authenticated [UserEntity]
 */
@Parameter(hidden = true) // do not generate API docs for this
annotation class AuthUserId