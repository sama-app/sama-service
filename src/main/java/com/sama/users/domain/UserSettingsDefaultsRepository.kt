package com.sama.users.domain

import com.sama.common.NotFoundException

interface UserSettingsDefaultsRepository {
    fun findByIdOrNull(userId: Long): UserSettingsDefaults?
    fun findByIdOrThrow(userId: Long) = findByIdOrNull(userId)
        ?: throw NotFoundException(UserSettingsDefaults::class, userId)
}