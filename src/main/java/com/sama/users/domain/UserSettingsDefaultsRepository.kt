package com.sama.users.domain

interface UserSettingsDefaultsRepository {
    fun findOne(userId: Long): UserSettingsDefaults
}