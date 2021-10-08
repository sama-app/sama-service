package com.sama.integration.google.calendar.application

import com.sama.common.component1
import com.sama.common.component2
import com.sama.common.to
import com.sama.connection.application.AddDiscoveredUsersCommand
import com.sama.connection.application.UserConnectionService
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.GoogleSyncTokenInvalidatedException
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.CalendarListRepository
import com.sama.integration.google.calendar.domain.CalendarListSync
import com.sama.integration.google.calendar.domain.CalendarListSyncRepository
import com.sama.integration.google.calendar.domain.CalendarSync
import com.sama.integration.google.calendar.domain.CalendarSyncRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.integration.google.calendar.domain.findAllCalendars
import com.sama.integration.google.calendar.domain.findAllEvents
import com.sama.integration.google.translatedGoogleException
import com.sama.users.application.UserSettingsService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPermission
import java.time.Clock
import liquibase.pro.packaged.it
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GoogleCalendarSyncer(
    private val googleServiceFactory: GoogleServiceFactory,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val calendarListSyncRepository: CalendarListSyncRepository,
    private val calendarListRepository: CalendarListRepository,
    private val userSettingsService: UserSettingsService,
    private val userConnectionService: UserConnectionService,
    private val clock: Clock
) {
    private var logger: Logger = LoggerFactory.getLogger(GoogleCalendarSyncer::class.java)

    @Transactional
    fun enableCalendarListSync(userId: UserId) {
        val calendarSync = calendarListSyncRepository.find(userId)
            ?.forceSync(clock)
            ?: CalendarListSync.new(userId, clock)
        calendarListSyncRepository.save(calendarSync)
        syncUserCalendarList(userId)
    }

    @Transactional
    fun syncUserCalendarList(userId: UserId, forceFullSync: Boolean = false) {
        val calendarSync = try {
            calendarListSyncRepository.findAndLock(userId)
                ?: CalendarListSync.new(userId, clock)
        } catch (e: CannotAcquireLockException) {
            logger.debug("User#${userId.id} CalendarList sync already in progress...")
            return
        }

        logger.debug("Syncing User#${userId.id} CalendarList...")
        val calendarService = googleServiceFactory.calendarService(userId)
        try {
            val (newCalendarList, newSyncToken) =
                if (forceFullSync || calendarSync.needsFullSync()) {
                    val (calendars, syncToken) = calendarService.findAllCalendars(null)
                    val newCalendarList = calendars.toDomain(userId)
                    newCalendarList to syncToken
                } else {
                    val (calendars, syncToken) = calendarService.findAllCalendars(calendarSync.syncToken)
                    val calendarListDiff = calendars.toDomain(userId)
                    val existingCalendarList = calendarListRepository.find(userId)!!
                    val newCalendarList = existingCalendarList.merge(calendarListDiff)
                    newCalendarList to syncToken
                }

            val currentlySyncedCalendars = calendarSyncRepository.findAllCalendarIds(userId)
            val calendarsToEnable = newCalendarList.syncableCalendars
                .minus(currentlySyncedCalendars)

            val calendarsToDisable = currentlySyncedCalendars
                .minus(newCalendarList.syncableCalendars)

            calendarsToEnable.forEach { enableCalendarSync(userId, it) }
            calendarsToDisable.forEach { disableCalendarSync(userId, it) }

            calendarListRepository.save(newCalendarList)

            val updatedSync = calendarSync.complete(newSyncToken!!, clock)
            calendarListSyncRepository.save(updatedSync)
            logger.debug("Completed sync for User#${userId.id} CalendarList...")
        } catch (e: Exception) {
            val ex = translatedGoogleException(e)
            if (ex is GoogleSyncTokenInvalidatedException) {
                logger.debug("CalendarList sync token expired for User#${userId.id}", e)
                val updatedSync = calendarSync.reset(clock)
                calendarListSyncRepository.save(updatedSync)
            } else {
                val updatedSync = calendarSync.fail(clock)
                logger.error("Failed to sync CalendarList for User#${userId.id} ${updatedSync.failedSyncCount} times", e)
                calendarListSyncRepository.save(updatedSync)
            }
        }

    }

    @Transactional
    fun enableCalendarSync(userId: UserId, calendarId: GoogleCalendarId) {
        val calendarSync = calendarSyncRepository.find(userId, calendarId)
            ?.forceSync(clock)
            ?: CalendarSync.new(userId, calendarId, clock)
        calendarSyncRepository.save(calendarSync)
    }

    @Transactional
    fun disableCalendarSync(userId: UserId, calendarId: GoogleCalendarId) {
        val calendarSync = calendarSyncRepository.find(userId, calendarId)
        calendarEventRepository.deleteBy(userId, calendarId)
        if (calendarSync != null) {
            calendarSyncRepository.delete(userId, calendarId)
        }
    }

    @Transactional
    fun syncUserCalendar(userId: UserId, calendarId: GoogleCalendarId, forceFullSync: Boolean = false) {
        val calendarSync = try {
            calendarSyncRepository.findAndLock(userId, calendarId)
                ?: CalendarSync.new(userId, calendarId, clock)
        } catch (e: CannotAcquireLockException) {
            logger.debug("User#${userId.id} Calendar#${calendarId} sync already in progress...")
            return
        }

        val grantedPermissions = userSettingsService.find(userId).grantedPermissions
        val pastEventContactScanEnabled = grantedPermissions.contains(UserPermission.PAST_EVENT_CONTACT_SCAN)

        logger.debug("Syncing User#${userId.id} Calendar#${calendarId}...")
        val calendarService = googleServiceFactory.calendarService(userId)
        try {
            val updatedSync = if (forceFullSync || calendarSync.needsFullSync(clock)) {
                val (startDate, endDate) = CalendarSync.syncRange(clock)
                val (events, timeZone, syncToken) = calendarService
                    .findAllEvents(calendarId, startDate, endDate)

                val calendarEvents = events.toDomain(userId, calendarId, timeZone)
                calendarEventRepository.deleteBy(userId, calendarId)
                calendarEventRepository.saveAll(calendarEvents)

                if (pastEventContactScanEnabled) {
                    val attendeeEmails = events.attendeeEmails()
                    userConnectionService.addDiscoveredUsers(userId, AddDiscoveredUsersCommand(attendeeEmails))
                }

                calendarSync.complete(syncToken!!, startDate to endDate, clock)
            } else {
                val (events, timeZone, newSyncToken) = calendarService
                    .findAllEvents(calendarId, calendarSync.syncToken!!)

                val (toAdd, toRemove) = events.partition { it.status in ACCEPTED_EVENT_STATUSES }
                calendarEventRepository.saveAll(toAdd.toDomain(userId, calendarId, timeZone))
                calendarEventRepository.deleteAll(toRemove.map { it.toKey(userId, calendarId) })

                if (pastEventContactScanEnabled) {
                    val attendeeEmails = events.attendeeEmails()
                    userConnectionService.addDiscoveredUsers(userId, AddDiscoveredUsersCommand(attendeeEmails))
                }

                calendarSync.complete(newSyncToken!!, clock)
            }

            calendarSyncRepository.save(updatedSync)
            logger.debug("Completed sync for User#${userId.id} Calendar#${calendarId}...")
        } catch (e: Exception) {
            val ex = translatedGoogleException(e)
            if (ex is GoogleSyncTokenInvalidatedException) {
                logger.debug("Calendar sync token expired for User#${userId.id}", e)
                val updated = calendarSync.reset(clock)
                calendarEventRepository.deleteBy(userId, calendarId)
                calendarSyncRepository.save(updated)
            } else {
                val updated = calendarSync.fail(clock)
                logger.error("Failed to sync Calendar for User#${userId.id} ${updated.failedSyncCount} times", e)
                calendarSyncRepository.save(updated)
            }
        }
    }

}