package com.sama.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
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
    private val googleHttpTransport: HttpTransport,
    private val jsonFactory: JsonFactory
) : UserSettingsDefaultsRepository {

    override fun findByIdOrNull(userId: Long): UserSettingsDefaults? {
        val settings = kotlin.runCatching {
            calendarServiceForUser(userId).settings()
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

    private fun calendarServiceForUser(userId: Long): Calendar {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
        return Calendar.Builder(googleHttpTransport, jsonFactory, credential)
            .setApplicationName(SamaApplication::class.simpleName)
            .build()
    }
}