package com.sama.integration.google.calendar.application

import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.auth.application.GoogleAccountService
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.users.domain.UserId
import com.sama.users.domain.UserSettingsDefaults
import com.sama.users.domain.UserSettingsDefaultsRepository
import java.time.ZoneId
import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component

@Component
class GoogleCalendarSettingsService(
    private val googleAccountRepository: GoogleAccountRepository,
    private val googleServiceFactory: GoogleServiceFactory
) : UserSettingsDefaultsRepository {

    override fun findById(userId: UserId): UserSettingsDefaults? {
        val settings = kotlin.runCatching {
            val googleAccountId = googleAccountRepository.findByUserIdAndPrimary(userId)!!
            googleServiceFactory.calendarService(googleAccountId).settings()
                .list().execute().items
                .associate { Pair(it.id, it.value) }
        }.getOrNull()

        return settings?.let {
            UserSettingsDefaults(
                settings["locale"]?.let { LocaleUtils.toLocale(it) },
                settings["timezone"]?.let { ZoneId.of(it) },
                settings["format24HourTime"]?.let { it.toBoolean() },
            )
        }
    }
}