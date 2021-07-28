package com.sama.comms.application

import com.sama.meeting.domain.MeetingConfirmedEvent
import org.springframework.stereotype.Component

@Component
class CommsEventConsumer(private val meetingCommsApplicationService: MeetingCommsApplicationService) {

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val confirmedMeeting = event.confirmedMeeting
        meetingCommsApplicationService.sendMeetingConfirmedComms(
            SendMeetingConfirmedCommand(
                confirmedMeeting.initiatorId,
                confirmedMeeting.meetingRecipient.email!!,
                confirmedMeeting.slot.startDateTime
            )
        )
    }
}