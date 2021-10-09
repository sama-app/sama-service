package com.sama.api.debug

import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.calendar.application.GoogleCalendarSyncer
import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class MigrationController(
    private val googleAccountRepository: GoogleAccountRepository,
    private val googleCalendarSyncer: GoogleCalendarSyncer
) {
    private var logger: Logger = LoggerFactory.getLogger(MigrationController::class.java)

    @PostMapping("/api/__migration/001-migrate-calendar-sync")
    fun migrateCalendarSync() {
        val accountIds = googleAccountRepository.findAllIds()
        accountIds.forEach { accountId ->
            kotlin.runCatching { googleCalendarSyncer.enableCalendarListSync(accountId) }
                .onSuccess { logger.info("Migrated GoogleAccount#$accountId") }
                .onFailure { logger.warn("Failed to migrate GoogleAccount$accountId: ${it.message}", it) }
        }
    }
}