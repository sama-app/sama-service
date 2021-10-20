package com.sama.calendar.application

import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.meeting.domain.MeetingProposedEvent
import org.springframework.stereotype.Component

@Component
class CalendarEventConsumer(private val eventService: EventService) {

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val meeting = event.confirmedMeeting
        eventService.createEvent(
            meeting.initiatorId,
            CreateEventCommand(
                meeting.meetingCode,
                meeting.slot.startDateTime,
                meeting.slot.endDateTime,
                event.confirmedMeeting.recipient,
                meeting.meetingTitle
            )
        )
    }

    fun onMeetingProposed(event: MeetingProposedEvent) {
        val meeting = event.proposedMeeting
        if (meeting.meetingPreferences.blockOutSlots) {
            val command = BlockOutTimesCommand(
                meeting.meetingCode,
                meeting.meetingTitle,
                meeting.proposedSlots
                    .map { Slot(it.startDateTime, it.endDateTime) })
            eventService.blockOutTimes(meeting.initiatorId, command)
        }
    }
}