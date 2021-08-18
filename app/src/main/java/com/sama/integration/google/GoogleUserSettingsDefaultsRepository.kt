package com.sama.integration.google

import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettingsDefaults
import com.sama.users.domain.UserSettingsDefaultsRepository
import java.time.ZoneId
import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component

@Component
class GoogleUserSettingsDefaultsRepository(
    private val googleServiceFactory: GoogleServiceFactory
) : UserSettingsDefaultsRepository {

    override fun findByIdOrNull(userId: UserId): UserSettingsDefaults? {
        val settings = kotlin.runCatching {
            googleServiceFactory.calendarService(userId).settings()
                .list().execute().items
                .associate { Pair(it.id, it.value) }
        }.getOrNull()

        return settings?.let {
            UserSettingsDefaults(
                settings["locale"]?.let { LocaleUtils.toLocale(it) },
                settings["timezone"]?.let { ZoneId.of(it) },
                settings["format24HourTime"]?.let { it.toBoolean() },
                null
            )
        }
    }
}