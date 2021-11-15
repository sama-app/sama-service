package com.sama.integration.google.calendar.application

import com.google.api.services.calendar.model.Calendar
import com.google.api.services.calendar.model.CalendarListEntry
import com.google.api.services.calendar.model.ConferenceData
import com.google.api.services.calendar.model.ConferenceSolutionKey
import com.google.api.services.calendar.model.CreateConferenceRequest
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.sama.common.ApplicationService
import com.sama.common.NotFoundException
import com.sama.common.checkAccess
import com.sama.integration.google.GoogleApiRateLimitException
import com.sama.integration.google.GoogleInternalServerException
import com.sama.integration.google.GoogleServiceFactory
import com.sama.integration.google.NoPrimaryGoogleAccountException
import com.sama.integration.google.auth.domain.GoogleAccount
import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.auth.domain.GoogleAccountRepository
import com.sama.integration.google.calendar.domain.AggregatedData
import com.sama.integration.google.calendar.domain.CalendarEventRepository
import com.sama.integration.google.calendar.domain.CalendarList
import com.sama.integration.google.calendar.domain.CalendarListRepository
import com.sama.integration.google.calendar.domain.CalendarListSyncRepository
import com.sama.integration.google.calendar.domain.CalendarSyncRepository
import com.sama.integration.google.calendar.domain.GoogleCalendarEvent
import com.sama.integration.google.calendar.domain.GoogleCalendarEventId
import com.sama.integration.google.calendar.domain.GoogleCalendarId
import com.sama.integration.google.calendar.domain.PRIMARY_CALENDAR_ID
import com.sama.integration.google.calendar.domain.findAllEvents
import com.sama.integration.google.calendar.domain.insert
import com.sama.integration.google.calendar.domain.toDomain
import com.sama.integration.google.calendar.domain.toGoogleCalendarDateTime
import com.sama.integration.google.translatedGoogleException
import com.sama.users.domain.UserId
import io.sentry.spring.tracing.SentryTransaction
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ApplicationService
class SyncGoogleCalendarService(
    private val googleServiceFactory: GoogleServiceFactory,
    private val googleAccountRepository: GoogleAccountRepository,
    private val calendarListRepository: CalendarListRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val calendarListSyncRepository: CalendarListSyncRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val googleCalendarSyncer: GoogleCalendarSyncer,
    private val clock: Clock,
) : GoogleCalendarService {
    private var logger: Logger = LoggerFactory.getLogger(javaClass)

    // https://developers.google.com/calendar/v3/reference/events/list
    // https://developers.google.com/calendar/v3/reference/events#resource
    override fun findEvents(
        userId: UserId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        createdFrom: ZonedDateTime?,
        hasAttendees: Boolean?
    ): List<CalendarEventDTO> {
        val linkedAccounts = googleAccountRepository.findAllByUserId(userId)
            .filter { it.linked }
            .associateBy { it.id!! }
        val calendarSyncs = calendarSyncRepository.findAll(linkedAccounts.keys)

        val calendarEvents = if (calendarSyncs.isNotEmpty()) {
            calendarSyncs
                .map { sync ->
                    val isSynced = sync.isSyncedFor(startDateTime, endDateTime, clock)
                    if (isSynced) {
                        val minAttendeeCount = if (hasAttendees == true) 1 else null
                        calendarEventRepository.findAll(
                            sync.accountId,
                            sync.calendarId,
                            startDateTime,
                            endDateTime,
                            createdFrom,
                            minAttendeeCount
                        )
                    } else {
                        forceLoadCalendarEvents(
                            linkedAccounts[sync.accountId]!!,
                            sync.calendarId,
                            startDateTime,
                            endDateTime,
                            createdFrom,
                            hasAttendees
                        )
                    }
                }
                .flatten()
        } else {
            // If there aren't any synced calendars, load the primary calendar for all accounts
            linkedAccounts.values
                .map { forceLoadCalendarEvents(it, PRIMARY_CALENDAR_ID, startDateTime, endDateTime, createdFrom, hasAttendees) }
                .flatten()
        }

        // Compute aggregate values
        val recurringEventCounts = calendarEvents
            .filter { it.eventData.recurringEventId != null }
            .groupingBy { it.eventData.recurringEventId }
            .eachCount()

        return calendarEvents.map { (key, start, end, eventData) ->
            val recurrenceCount = eventData.recurringEventId?.let { recurringEventCounts[it] } ?: 0
            CalendarEventDTO(
                linkedAccounts[key.accountId]!!.publicId!!, key.calendarId, key.eventId,
                start, end, eventData, AggregatedData(recurrenceCount)
            )
        }
    }

    override fun findEventIdsByExtendedProperties(
        userId: UserId, extendedProperties: Map<String, String>, useSamaCalendar: Boolean
    ): List<GoogleCalendarEventId> {
        val primaryAccountId = googleAccountRepository.findByUserIdAndPrimary(userId)
            ?: throw NoPrimaryGoogleAccountException(userId)

        val calendarId = targetCalendarId(useSamaCalendar, primaryAccountId)
        val calendarService = googleServiceFactory.calendarService(primaryAccountId)

        return calendarService.events().list(calendarId)
            .apply {
                maxResults = 250
                privateExtendedProperty = extendedProperties.map { (k, v) -> "$k=$v" }
                fields = "items(id)"
            }.execute()
            .items
            .map { it.id }
    }

    private fun forceLoadCalendarEvents(
        account: GoogleAccount,
        calendarId: GoogleCalendarId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        createdFrom: ZonedDateTime?,
        hasAttendees: Boolean?
    ) = try {
        val calendarService = googleServiceFactory.calendarService(account.id!!)
        val minAttendeeCount = if (hasAttendees == true) 1 else null
        val (events, timeZone) = calendarService.findAllEvents(
            calendarId, startDateTime, endDateTime,
            createdFrom = createdFrom, minAttendeeCount = minAttendeeCount
        )
        events.toDomain(account, PRIMARY_CALENDAR_ID, timeZone)
    } catch (e: Exception) {
        logger.error("GoogleCalendarService#forceLoadCalendarEvents", e)
        throw translatedGoogleException(e)
    }

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    // https://developers.google.com/calendar/api/v3/reference/events/insert#request-body
    @Retryable(
        value = [GoogleInternalServerException::class, GoogleApiRateLimitException::class],
        maxAttempts = 10, backoff = Backoff(delay = 1000, multiplier = 2.0, maxDelay = 60000)
    )
    override fun insertEvent(userId: UserId, command: InsertGoogleCalendarEventCommand): Boolean {
        try {
            val timeZone = command.startDateTime.zone
            val googleCalendarEvent = GoogleCalendarEvent().apply {
                start = EventDateTime()
                    .setDateTime(command.startDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                end = EventDateTime()
                    .setDateTime(command.endDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                attendees = command.attendees.map { attendee ->
                    EventAttendee().apply {
                        email = attendee.email
                        responseStatus = "accepted"
                    }
                }
                summary = command.title
                description = command.description
                conferenceData = command.conferenceType?.let { conferenceType ->
                    ConferenceData().apply {
                        createRequest = CreateConferenceRequest().apply {
                            requestId = UUID.randomUUID().toString()
                            conferenceSolutionKey = ConferenceSolutionKey().apply {
                                type = when (conferenceType) {
                                    ConferenceType.GOOGLE_MEET -> "hangoutsMeet"
                                }
                            }
                        }
                    }
                }
                extendedProperties = Event.ExtendedProperties().apply {
                    private = command.privateExtendedProperties
                }
            }

            val primaryAccountId = googleAccountRepository.findByUserIdAndPrimary(userId)
                ?: throw NoPrimaryGoogleAccountException(userId)
            val calendarId = targetCalendarId(command.useSamaCalendar, primaryAccountId)

            val calendarService = googleServiceFactory.calendarService(primaryAccountId)
            calendarService.insert(calendarId, googleCalendarEvent)

            googleCalendarSyncer.syncUserCalendar(primaryAccountId, calendarId)
            return true
        } catch (e: Exception) {
            logger.error("GoogleCalendarService#insertEvent", e)
            throw translatedGoogleException(e)
        }
    }

    @Retryable(
        value = [GoogleInternalServerException::class, GoogleApiRateLimitException::class],
        maxAttempts = 10, backoff = Backoff(delay = 1000, multiplier = 2.0, maxDelay = 60000)
    )
    override fun deleteEvent(userId: UserId, command: DeleteGoogleCalendarEventCommand): Boolean {
        try {
            val primaryAccountId = googleAccountRepository.findByUserIdAndPrimary(userId)
                ?: throw NoPrimaryGoogleAccountException(userId)
            val calendarId = targetCalendarId(command.useSamaCalendar, primaryAccountId)

            val calendarService = googleServiceFactory.calendarService(primaryAccountId)

            calendarService.events().delete(calendarId, command.eventId).execute()
            return true
        } catch (e: Exception) {
            logger.error("GoogleCalendarService#deleteEvent", e)
            throw translatedGoogleException(e)
        }
    }

    @Retryable(
        value = [GoogleInternalServerException::class, GoogleApiRateLimitException::class],
        maxAttempts = 10, backoff = Backoff(delay = 1000, multiplier = 2.0, maxDelay = 60000)
    )
    override fun createSamaCalendar(userId: UserId) {
        val accountId = googleAccountRepository.findByUserIdAndPrimary(userId)
            ?: throw NoPrimaryGoogleAccountException(userId)
        val samaCalendar = calendarListRepository.find(accountId)?.samaCalendar()
        if (samaCalendar != null) {
            return
        }

        val calendarService = googleServiceFactory.calendarService(accountId)
        val calendarId = try {
            calendarService.Calendars().insert(
                Calendar().apply {
                    summary = "Sama"
                    description = "Calendar managed by Sama"
                }
            ).execute().id
        } catch (e: Exception) {
            logger.error("GoogleCalendarService#createSamaCalendar", e)
            throw translatedGoogleException(e)
        }

        try {
            calendarService.calendarList().patch(
                calendarId,
                CalendarListEntry().apply {
                    accessRole = "owner"
                    backgroundColor = "#E5E4E2"
                    selected = true
                }
            ).apply { colorRgbFormat = true }
                .execute()
        } catch (ignored: Exception) {
            logger.warn("Could not update Sama Calendar for User#${userId.id}")
            // The calendar was created, it's okay if the colour is not updated due to an error
            // as we'd need to write rollback logic
        }
    }

    private fun targetCalendarId(useSamaCalendar: Boolean, primaryAccountId: GoogleAccountId) =
        if (!useSamaCalendar) {
            PRIMARY_CALENDAR_ID
        } else {
            calendarListRepository.find(primaryAccountId)?.samaCalendarId ?: PRIMARY_CALENDAR_ID
        }

    @Transactional(readOnly = true)
    override fun findCalendars(userId: UserId): CalendarsDTO {
        val accountIds = googleAccountRepository.findAllByUserId(userId)
            .associateBy { it.id!! }

        return calendarListRepository.findAll(accountIds.keys)
            .flatMap { (googleAccountId, calendars, selected) ->
                val accountId = accountIds[googleAccountId]!!.publicId!!
                calendars.map { (calendarId, calendar) ->
                    CalendarDTO(accountId, calendarId, calendarId in selected, calendar.summary, calendar.backgroundColor)
                }
            }
            .let { CalendarsDTO(it) }
    }

    @Transactional
    override fun addSelectedCalendar(userId: UserId, command: AddSelectedCalendarCommand): Boolean {
        val googleAccount = googleAccountRepository.findByPublicIdOrThrow(command.accountId)
        checkAccess(googleAccount.userId == userId)

        val calendarList = calendarListRepository.find(googleAccount.id!!)
            ?: throw NotFoundException(CalendarList::class, command.accountId)

        calendarList.addSelection(command.calendarId)
            .also { calendarListRepository.save(it) }

        googleCalendarSyncer.enableCalendarSync(googleAccount.id, command.calendarId)
        return true
    }

    @Transactional
    override fun removeSelectedCalendar(userId: UserId, command: RemoveSelectedCalendarCommand): Boolean {
        val googleAccount = googleAccountRepository.findByPublicIdOrThrow(command.accountId)
        checkAccess(googleAccount.userId == userId)

        val calendarList = calendarListRepository.find(googleAccount.id!!)
            ?: throw NotFoundException(CalendarList::class, command.accountId)

        calendarList.removeSelection(command.calendarId)
            .also { calendarListRepository.save(it) }

        googleCalendarSyncer.disableCalendarSync(googleAccount.id, command.calendarId)
        return true
    }

    @SentryTransaction(operation = "syncUserCalendarLists")
    @Scheduled(initialDelay = 30000, fixedDelay = 25000)
    fun syncUserCalendarLists() {
        val userCalendarListsToSync = calendarListSyncRepository.findSyncable(Instant.now())
        userCalendarListsToSync.forEach { userId ->
            googleCalendarSyncer.syncUserCalendarList(userId)
        }
        logger.info("Synced ${userCalendarListsToSync.size} Google Calendar lists")
    }

    @SentryTransaction(operation = "syncUserCalendars")
    @Scheduled(initialDelay = 30000, fixedDelay = 25000)
    fun syncUserCalendars() {
        val userCalendarsToSync = calendarSyncRepository.findSyncable(Instant.now())
        userCalendarsToSync.forEach { (accountId, calendarId) ->
            googleCalendarSyncer.syncUserCalendar(accountId, calendarId)
        }
        logger.info("Synced ${userCalendarsToSync.size} Google Calendars")
    }
}