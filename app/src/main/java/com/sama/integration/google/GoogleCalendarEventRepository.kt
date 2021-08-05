package com.sama.integration.google

import com.google.api.services.calendar.model.ConferenceData
import com.google.api.services.calendar.model.ConferenceSolutionKey
import com.google.api.services.calendar.model.CreateConferenceRequest
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.sama.calendar.domain.Event
import com.sama.calendar.domain.EventRepository
import com.sama.slotsuggestion.domain.Block
import com.sama.slotsuggestion.domain.BlockRepository
import com.sama.slotsuggestion.domain.Recurrence
import com.sama.users.domain.UserId
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import org.dmfs.rfc5545.recur.Freq
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.springframework.stereotype.Component

@Component
class GoogleCalendarEventRepository(private val googleServiceFactory: GoogleServiceFactory) : EventRepository,
    BlockRepository {

    // https://developers.google.com/calendar/api/v3/reference/events/insert
    // https://developers.google.com/calendar/api/v3/reference/events/insert#request-body
    override fun save(userId: UserId, event: Event): Event {
        return try {
            val calendarService = googleServiceFactory.calendarService(userId)
            val timeZone = event.startDateTime.zone
            val googleCalendarEvent = GoogleCalendarEvent().apply {
                start = EventDateTime()
                    .setDateTime(event.startDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                end = EventDateTime()
                    .setDateTime(event.endDateTime.toGoogleCalendarDateTime())
                    .setTimeZone(timeZone.id)
                attendees = listOf(
                    EventAttendee().apply {
                        email = event.recipientEmail
                    },
                )
                summary = event.title
                description = event.description
                conferenceData = ConferenceData().apply {
                    createRequest = CreateConferenceRequest().apply {
                        requestId = UUID.randomUUID().toString()
                        conferenceSolutionKey = ConferenceSolutionKey().apply {
                            type = "hangoutsMeet"
                        }

                    }
                }
            }

            val inserted = calendarService.events()
                .insert("primary", googleCalendarEvent)
                .setSendNotifications(true)
                .setConferenceDataVersion(1)
                .execute()
            inserted.toEvent(timeZone)
        } catch (e: Exception) {
            throw translatedGoogleException(e)
        }
    }

    // https://developers.google.com/calendar/v3/reference/events/list
    // https://developers.google.com/calendar/v3/reference/events#resource
    override fun findAll(userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): List<Event> {
        return try {
            val calendarService = googleServiceFactory.calendarService(userId)
            val (calendarEvents, calendarTimeZone) = calendarService.findAllEvents(startDateTime, endDateTime)
            calendarEvents.map { it.toEvent(calendarTimeZone) }
        } catch (e: Exception) {
            throw translatedGoogleException(e)
        }
    }

    private fun GoogleCalendarEvent.toEvent(calendarTimeZone: ZoneId): Event {
        return Event(
            this.start.toZonedDateTime(calendarTimeZone),
            this.end.toZonedDateTime(calendarTimeZone),
            this.isAllDay(),
            this.summary,
            this.description,
            if (this.attendees != null) {
                this.attendees.firstOrNull()?.email
            } else {
                null
            }
        )
    }

    override fun findAllBlocks(
        userId: Long,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime
    ): Collection<Block> {
        return try {
            val calendarService = googleServiceFactory.calendarService(userId)

            val (calendarEvents, calendarTimeZone) = calendarService.findAllEvents(startDateTime, endDateTime)
            val recurrenceRulesByEventID = calendarService.recurrenceRulesFor(calendarEvents)
            val recurringEventCounts = calendarEvents
                .filter { it.recurringEventId != null }
                .groupingBy { it.recurringEventId }
                .eachCount()

            calendarEvents
                .map { it.toBlock(calendarTimeZone, recurringEventCounts, recurrenceRulesByEventID) }
        } catch (e: Exception) {
            throw translatedGoogleException(e)
        }
    }

    private fun GoogleCalendarEvent.toBlock(
        calendarTimeZone: ZoneId,
        recurrenceCounts: Map<String, Int>,
        recurringEvents: Map<String, RecurrenceRule?>
    ): Block {
        val hasRecipients = this.attendees != null && this.attendees.size > 0

        val recurrenceCount = recurringEventId
            ?.let { recurrenceCounts[it] }
            ?: 1

        val recurrenceRule = recurringEventId
            ?.let { recurringEvents[it] }
            ?.takeIf { it.freq in listOf(Freq.DAILY, Freq.WEEKLY, Freq.MONTHLY, Freq.YEARLY) }
            ?.let {
                com.sama.slotsuggestion.domain.RecurrenceRule(
                    Recurrence.valueOf(it.freq.name),
                    it.interval
                )
            }

        return Block(
            this.start.toZonedDateTime(calendarTimeZone),
            this.end.toZonedDateTime(calendarTimeZone),
            this.isAllDay(),
            hasRecipients,
            recurrenceCount,
            recurrenceRule
        )
    }

}