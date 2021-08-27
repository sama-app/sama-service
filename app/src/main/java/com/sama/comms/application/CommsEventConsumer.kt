package com.sama.comms.application

import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.meeting.domain.UserRecipient
import org.springframework.stereotype.Component

@Component
class CommsEventConsumer(private val meetingCommsApplicationService: MeetingCommsApplicationService) {

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val recipientEmail = when (val recipient = event.confirmedMeeting.meetingRecipient) {
            is EmailRecipient -> recipient.email
            is UserRecipient -> recipient.email
        }

        val confirmedMeeting = event.confirmedMeeting
        meetingCommsApplicationService.sendMeetingConfirmedComms(
            SendMeetingConfirmedCommand(
                confirmedMeeting.initiatorId,
                recipientEmail,
                confirmedMeeting.slot.startDateTime
            )
        )
    }
}