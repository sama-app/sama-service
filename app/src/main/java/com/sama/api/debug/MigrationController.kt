package com.sama.api.debug

import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.calendar.application.GoogleCalendarSyncer
import com.sama.integration.google.calendar.domain.CalendarListRepository
import io.swagger.v3.oas.annotations.Hidden
import java.time.Instant
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class MigrationController(
    private val googleAccountRepository: GoogleAccountRepository,
    private val calendarListRepository: CalendarListRepository,
    private val googleCalendarSyncer: GoogleCalendarSyncer,
    private val taskScheduler: TaskScheduler
) {
    private var logger: Logger = LoggerFactory.getLogger(MigrationController::class.java)

    @PostMapping("/api/__migration/001-migrate-calendar-sync")
    fun migrateCalendarSync(): String {
        taskScheduler.schedule(
            {
                val accountIds = googleAccountRepository.findAllIds()
                accountIds.forEach { accountId ->
                    kotlin.runCatching { googleCalendarSyncer.enableCalendarListSync(accountId) }
                        .onSuccess { logger.info("Migrated CalendarList for GoogleAccount#${accountId.id}") }
                        .onFailure {
                            logger.warn(
                                "Failed to migrate CalendarList for GoogleAccount$accountId: ${it.message}",
                                it
                            )
                        }

                    calendarListRepository.find(accountId)
                        ?.syncableCalendars
                        ?.forEach { calendarId ->
                            kotlin.runCatching { googleCalendarSyncer.enableCalendarSync(accountId, calendarId) }
                                .onSuccess { logger.info("Migrated Calendar#$calendarId for GoogleAccount#${accountId.id}") }
                                .onFailure {
                                    logger.warn(
                                        "Failed to migrate Calendar#$calendarId for GoogleAccount$accountId: ${it.message}",
                                        it
                                    )
                                }
                        }
                }
            },
            Instant.now()
        )
        return "OK"
    }
}