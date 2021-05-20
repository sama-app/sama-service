package com.sama.api.common

import com.sama.users.domain.User
import io.swagger.v3.oas.annotations.Parameter

/**
 * Annotation to retrieve an ID an of authenticated [User]
 */
@Parameter(hidden = true) // do not generate API docs for this
annotation class AuthUserId