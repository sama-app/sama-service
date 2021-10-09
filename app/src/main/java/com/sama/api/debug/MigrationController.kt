package com.sama.api.debug

import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.calendar.application.GoogleCalendarSyncer
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class MigrationController(
    private val googleAccountRepository: GoogleAccountRepository,
    private val googleCalendarSyncer: GoogleCalendarSyncer
) {

    @PostMapping("/api/__migration/001-migrate-calendar-sync")
    fun migrateCalendarSync() {
        val accountIds = googleAccountRepository.findAllIds()
        accountIds.forEach {
            googleCalendarSyncer.enableCalendarListSync(it)
        }
    }
}