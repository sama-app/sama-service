package com.sama.calendar.application

import com.sama.common.ApplicationService
import com.sama.integration.google.calendar.application.DeleteGoogleCalendarEventCommand
import com.sama.integration.google.calendar.application.EventAttendee
import com.sama.integration.google.calendar.application.GoogleCalendarService
import com.sama.integration.google.calendar.application.InsertGoogleCalendarEventCommand
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.configuration.toUrl
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.UserRecipient
import com.sama.users.application.AuthUserService
import com.sama.users.application.InternalUserService
import com.sama.users.domain.UserId
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private const val MEETING_CODE_PROPERTY_KEY = "meeting_code"
private const val EVENT_TYPE_PROPERTY_KEY = "event_type"
private const val TYPE_MEETING_BLOCK = "meeting_block"

@ApplicationService
@Service
class EventApplicationService(
    private val googleCalendarService: GoogleCalendarService,
    private val internalUserService: InternalUserService,
    private val authUserService: AuthUserService,
    private val meetingUrlConfiguration: MeetingUrlConfiguration,
    @Value("\${sama.landing.url}") private val samaWebUrl: String,
) : EventService {

    override fun fetchEvents(
        userId: UserId,
        startDate: LocalDate,
        endDate: LocalDate,
        timezone: ZoneId,
        criteria: EventSearchCriteria
    ): EventsDTO {
        val events = googleCalendarService.findEvents(
            userId = userId,
            startDateTime = startDate.atStartOfDay(timezone),
            endDateTime = endDate.plusDays(1).atStartOfDay(timezone),
            createdFrom = criteria.createdFrom,
            hasAttendees = criteria.hasAttendees
        )
        return events
            .map {
                val isMeetingBlock = it.eventData.privateExtendedProperties[EVENT_TYPE_PROPERTY_KEY] == TYPE_MEETING_BLOCK
                EventDTO(
                    it.startDateTime, it.endDateTime, it.eventData.allDay, it.eventData.title, it.accountId,
                    it.calendarId, it.eventId, isMeetingBlock
                )
            }
            .let { EventsDTO(it) }
    }

    override fun fetchEvents(startDate: LocalDate, endDate: LocalDate, timezone: ZoneId, criteria: EventSearchCriteria) =
        fetchEvents(authUserService.currentUserId(), startDate, endDate, timezone, criteria)

    override fun createEvent(userId: UserId, command: CreateEventCommand): Boolean {
        val initiator = internalUserService.findInternal(userId)
        val initiatorEmail = initiator.email
        val timeZone = initiator.settings.timeZone
        val recipientEmail = when (val recipient = command.recipient) {
            is EmailRecipient -> recipient.email
            is UserRecipient -> internalUserService.find(recipient.recipientId).email
        }

        val extendedProperties = mapOf(
            MEETING_CODE_PROPERTY_KEY to command.meetingCode.code,
            EVENT_TYPE_PROPERTY_KEY to TYPE_MEETING_BLOCK
        )

        val blockedOutTimesEventIds = googleCalendarService.findEventIdsByExtendedProperties(userId, extendedProperties, true)

        val insertCommand = InsertGoogleCalendarEventCommand(
            command.startDateTime.withZoneSameInstant(timeZone),
            command.endDateTime.withZoneSameInstant(timeZone),
            command.title,
            description = "Time for this meeting was created via <a href=$samaWebUrl>Sama app</a>",
            listOf(EventAttendee(initiatorEmail), EventAttendee(recipientEmail)),
            useSamaCalendar = false,
        )

        runBlocking(Dispatchers.IO) {
            val insertEvent = async {
                googleCalendarService.insertEvent(userId, insertCommand)
            }

            blockedOutTimesEventIds.forEach {
                launch { googleCalendarService.deleteEvent(userId, DeleteGoogleCalendarEventCommand(it, true)) }
            }

            insertEvent.await()
        }

        return true
    }

    override fun blockOutTimes(userId: UserId, command: BlockOutTimesCommand) {
        if (command.slots.isEmpty()) {
            return
        }

        val meetingCode = command.meetingCode.code

        val commands = command.slots
            .map {
                InsertGoogleCalendarEventCommand(
                    it.startDateTime,
                    it.endDateTime,
                    "Reserved by Sama",
                    description = """
                        This time is reserved for ${command.meetingTitle} via <a href=$samaWebUrl>Sama app</a> until the recipient confirms their selection.
                        
                        You can view all suggested times for this meeting here: ${command.meetingCode.toUrl(meetingUrlConfiguration)}
                    """.trimIndent(),
                    attendees = emptyList(),
                    conferenceType = null,
                    privateExtendedProperties = mapOf(
                        MEETING_CODE_PROPERTY_KEY to meetingCode,
                        EVENT_TYPE_PROPERTY_KEY to TYPE_MEETING_BLOCK
                    ),
                    true
                )
            }

        runBlocking(Dispatchers.IO) {
            commands.forEach { launch { googleCalendarService.insertEvent(userId, it) } }
        }
    }
}
