package com.sama.integration.google.calendar.application

import com.sama.common.component1
import com.sama.common.component2
import com.sama.common.execute
import com.sama.common.to
import com.sama.comms.application.CommsEventConsumer
import com.sama.connection.application.AddDiscoveredUsersCommand
import com.sama.connection.application.UserConnectionService
import com.sama.integration.google.GoogleInvalidCredentialsException
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.GoogleSyncTokenInvalidatedException
import com.sama.integration.google.SyncConfiguration
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.calendar.domain.ACCEPTED_EVENT_STATUSES
import com.sama.integration.google.calendar.domain.CalendarDatesUpdatedEvent
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.CalendarList
import com.sama.integration.google.calendar.domain.CalendarListRepository
import com.sama.integration.google.calendar.domain.CalendarListSync
import com.sama.integration.google.calendar.domain.CalendarListSyncRepository
import com.sama.integration.google.calendar.domain.CalendarSync
import com.sama.integration.google.calendar.domain.CalendarSyncRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.integration.google.calendar.domain.ResourceType.CALENDAR
import com.sama.integration.google.calendar.domain.ResourceType.CALENDAR_LIST
import com.sama.integration.google.calendar.domain.attendeeEmails
import com.sama.integration.google.calendar.domain.findAllCalendars
import com.sama.integration.google.calendar.domain.findAllEvents
import com.sama.integration.google.calendar.domain.toDomain
import com.sama.integration.google.calendar.domain.toKey
import com.sama.integration.google.translatedGoogleException
import com.sama.users.application.UserSettingsService
import com.sama.users.domain.UserPermission
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.scheduling.TaskScheduler
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
    private val googleAccountRepository: GoogleAccountRepository,
    private val userConnectionService: UserConnectionService,
    private val channelManager: GoogleChannelManager,
    private val syncConfiguration: SyncConfiguration,
    private val commsEventConsumer: CommsEventConsumer,
    private val taskScheduler: TaskScheduler,
    private val clock: Clock
) {
    private var logger: Logger = LoggerFactory.getLogger(GoogleCalendarSyncer::class.java)

    @Transactional
    fun enableCalendarListSync(accountId: GoogleAccountId) {
        val calendarSync = calendarListSyncRepository.find(accountId)
            ?.forceSync(clock)
            ?: CalendarListSync.new(accountId, clock)
        calendarListSyncRepository.save(calendarSync)
        syncUserCalendarList(accountId)
        channelManager.createChannel(accountId, CALENDAR_LIST)
    }

    @Transactional
    fun disableCalendarListSync(accountId: GoogleAccountId) {
        channelManager.closeChannels(accountId, CALENDAR_LIST)
        calendarListRepository.delete(accountId)
        calendarListSyncRepository.deleteBy(accountId)
        val syncedCalendars = calendarSyncRepository.findAllCalendarIds(accountId)
        syncedCalendars.forEach { disableCalendarSync(accountId, it) }
    }

    @Transactional
    fun markCalendarListNeedsSync(accountId: GoogleAccountId) {
        val calendarSync = calendarListSyncRepository.find(accountId)?.forceSync(clock)
        requireNotNull(calendarSync)
        calendarListSyncRepository.save(calendarSync)
    }

    @Transactional
    fun syncUserCalendarList(accountId: GoogleAccountId, forceFullSync: Boolean = false) {
        val calendarListSync = try {
            calendarListSyncRepository.findAndLock(accountId)
                ?: CalendarListSync.new(accountId, clock)
        } catch (e: CannotAcquireLockException) {
            logger.info("GoogleAccount${accountId.id} CalendarList sync already in progress...")
            return
        }

        if (!calendarListSync.needsSync(clock)) {
            return
        }

        logger.debug("Syncing GoogleAccount${accountId.id} CalendarList...")
        val calendarService = googleServiceFactory.calendarService(accountId)
        try {
            val existingCalendarList = calendarListRepository.find(accountId)
            val syncToken = if (forceFullSync || calendarListSync.needsFullSync()) null else calendarListSync.syncToken

            val (calendars, newSyncToken) = calendarService.findAllCalendars(syncToken)
            val updatedCalendarList = existingCalendarList
                ?.mergeFromSource(calendars)
                ?: CalendarList.new(accountId, calendars)

            val currentlySyncedCalendars = calendarSyncRepository.findAllCalendarIds(accountId)
            val calendarsToEnable = updatedCalendarList.selected
                .minus(currentlySyncedCalendars)
            val calendarsToDisable = currentlySyncedCalendars
                .minus(updatedCalendarList.selected)

            calendarsToEnable.forEach { enableCalendarSync(accountId, it) }
            calendarsToDisable.forEach { disableCalendarSync(accountId, it) }

            calendarListRepository.save(updatedCalendarList)

            val updatedSync = calendarListSync.complete(newSyncToken!!, syncConfiguration, clock)
            calendarListSyncRepository.save(updatedSync)
            logger.info("Completed sync for GoogleAccount${accountId.id} CalendarList...")
        } catch (e: Exception) {
            when (translatedGoogleException(e)) {
                is GoogleSyncTokenInvalidatedException -> {
                    logger.info("CalendarList sync token expired for GoogleAccount${accountId.id}", e)
                    val updatedSync = calendarListSync.reset(clock)
                    calendarListSyncRepository.save(updatedSync)
                }
                is GoogleInvalidCredentialsException -> {
                    logger.info("Disabling CalendarList sync for GoogleAccount${accountId.id}", e)
                    disableCalendarListSync(accountId)
                }
                else -> {
                    val updatedSync = calendarListSync.fail(syncConfiguration, clock)
                    logger.error(
                        "Failed to sync CalendarList for GoogleAccount#${accountId.id} ${updatedSync.failedSyncCount} times",
                        e
                    )
                    calendarListSyncRepository.save(updatedSync)
                }
            }
        }
    }

    @Transactional
    fun enableCalendarSync(accountId: GoogleAccountId, calendarId: GoogleCalendarId) {
        val calendarSync = calendarSyncRepository.find(accountId, calendarId)
            ?.forceSync(clock)
            ?: CalendarSync.new(accountId, calendarId, clock)
        calendarSyncRepository.save(calendarSync)
        channelManager.createChannel(accountId, CALENDAR, calendarId)
    }

    @Transactional
    fun disableCalendarSync(accountId: GoogleAccountId, calendarId: GoogleCalendarId) {
        channelManager.closeChannels(accountId, CALENDAR, calendarId)
        val calendarSync = calendarSyncRepository.find(accountId, calendarId)
        calendarEventRepository.deleteBy(accountId, calendarId)
        if (calendarSync != null) {
            calendarSyncRepository.delete(accountId, calendarId)
        }
    }

    @Transactional
    fun markCalendarNeedsSync(accountId: GoogleAccountId, calendarId: GoogleCalendarId) {
        val calendarSync = calendarSyncRepository.find(accountId, calendarId)?.forceSync(clock)
        requireNotNull(calendarSync)
        calendarSyncRepository.save(calendarSync)
    }

    @Transactional
    fun syncUserCalendar(accountId: GoogleAccountId, calendarId: GoogleCalendarId, forceFullSync: Boolean = false) {
        val calendarSync = try {
            calendarSyncRepository.findAndLock(accountId, calendarId)
                ?: CalendarSync.new(accountId, calendarId, clock)
        } catch (e: CannotAcquireLockException) {
            logger.info("GoogleAccount${accountId.id} Calendar#${calendarId} sync already in progress...")
            return
        }

        if (!calendarSync.needsSync(clock)) {
            return
        }

        logger.debug("Syncing GoogleAccount${accountId.id} Calendar#${calendarId}...")
        val googleAccount = googleAccountRepository.findByIdOrThrow(accountId)
        if (!googleAccount.linked) {
            return
        }
        val userId = googleAccount.userId

        val grantedPermissions = userSettingsService.find(userId).grantedPermissions
        val pastEventContactScanEnabled = grantedPermissions.contains(UserPermission.PAST_EVENT_CONTACT_SCAN)

        val calendarService = googleServiceFactory.calendarService(accountId)
        try {
            val updatedSync = if (forceFullSync || calendarSync.needsFullSync(clock)) {
                val (startDate, endDate) = CalendarSync.syncRange(clock)
                val (events, timeZone, syncToken) = calendarService
                    .findAllEvents(calendarId, startDate, endDate)

                val calendarEvents = events.toDomain(googleAccount, calendarId, timeZone)
                calendarEventRepository.deleteBy(accountId, calendarId)
                calendarEventRepository.saveAll(calendarEvents)

                if (pastEventContactScanEnabled) {
                    val attendeeEmails = events.attendeeEmails()
                    userConnectionService.addDiscoveredUsers(userId, AddDiscoveredUsersCommand(attendeeEmails))
                }

                calendarSync.complete(syncToken!!, startDate to endDate, syncConfiguration, clock)
            } else {
                val (events, timeZone, newSyncToken) = calendarService
                    .findAllEvents(calendarId, calendarSync.syncToken!!)

                val (toAdd, toRemove) = events.partition { it.status in ACCEPTED_EVENT_STATUSES }
                val addedOrUpdatedEvents = toAdd.toDomain(googleAccount, calendarId, timeZone)
                calendarEventRepository.saveAll(addedOrUpdatedEvents)

                val keysToRemove = toRemove.map { it.toKey(accountId, calendarId) }
                val removedEvents = calendarEventRepository.findAll(keysToRemove)
                calendarEventRepository.deleteAll(keysToRemove)

                if (pastEventContactScanEnabled) {
                    val attendeeEmails = events.attendeeEmails()
                    userConnectionService.addDiscoveredUsers(userId, AddDiscoveredUsersCommand(attendeeEmails))
                }

                taskScheduler.execute {
                    val updatedDates = addedOrUpdatedEvents.plus(removedEvents)
                        .filter {
                            val now = ZonedDateTime.now(clock)
                            it.startDateTime.isBefore(now.plusMonths(3)) && it.startDateTime.isAfter(now.minusMonths(1))
                        }
                        .mapTo(mutableSetOf<LocalDate>()) { it.startDateTime.toLocalDate() }
                    if (updatedDates.isEmpty()) return@execute
                    commsEventConsumer.onCalendarDatesUpdated(CalendarDatesUpdatedEvent(userId, updatedDates))
                }

                calendarSync.complete(newSyncToken!!, syncConfiguration, clock)
            }

            calendarSyncRepository.save(updatedSync)
            logger.info("Completed sync for GoogleAccount${accountId.id} Calendar#${calendarId}...")
        } catch (e: Exception) {
            when (translatedGoogleException(e)) {
                is GoogleSyncTokenInvalidatedException -> {
                    logger.info("Calendar sync token expired for GoogleAccount${accountId.id}", e)
                    val updated = calendarSync.reset(clock)
                    calendarEventRepository.deleteBy(accountId, calendarId)
                    calendarSyncRepository.save(updated)
                }
                is GoogleInvalidCredentialsException -> {
                    logger.info("Disabling Calendar sync for GoogleAccount${accountId.id}", e)
                    disableCalendarSync(accountId, calendarId)
                }
                else -> {
                    val updated = calendarSync.fail(syncConfiguration, clock)
                    logger.error("Failed to sync Calendar for GoogleAccount${accountId.id} ${updated.failedSyncCount} times", e)
                    calendarSyncRepository.save(updated)
                }
            }
        }
    }

}