package com.sama.calendar.application

import com.sama.meeting.domain.MeetingConfirmedEvent
import org.springframework.stereotype.Component

@Component
class BlockEventConsumer(private val blockApplicationService: BlockApplicationService) {

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val meeting = event.confirmedMeeting
        blockApplicationService.createBlock(
            meeting.initiatorId,
            CreateBlockCommand(meeting.slot.startTime, meeting.slot.endTime, meeting.meetingRecipient.email!!)
        )
    }
}