package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.Calendar
import com.sama.SamaApplication
import com.sama.users.domain.UserSettingsDefaults
import com.sama.users.domain.UserSettingsDefaultsRepository
import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class GoogleUserSettingsDefaultsRepository(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val googleNetHttpTransport: HttpTransport,
    private val googleJacksonFactory: JacksonFactory
) : UserSettingsDefaultsRepository {

    override fun findOne(userId: Long): UserSettingsDefaults {
        val calendar = calendarServiceForUser(userId)
        val settings = calendar.settings().list().execute().items
            .associate { Pair(it.id, it.value) }
        return UserSettingsDefaults(
            settings["locale"]?.let { LocaleUtils.toLocale(it) },
            settings["timezone"]?.let { ZoneId.of(it) },
            settings["format24HourTime"]?.let { it.toBoolean() },
            null
        )
    }

    private fun calendarServiceForUser(userId: Long): Calendar {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
        return Calendar.Builder(googleNetHttpTransport, googleJacksonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }
}