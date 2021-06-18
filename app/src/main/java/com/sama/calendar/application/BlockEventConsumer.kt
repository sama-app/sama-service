package com.sama.calendar.application

import com.google.common.eventbus.Subscribe
import com.sama.events.EventConsumer
import com.sama.meeting.domain.MeetingConfirmedEvent
import org.springframework.stereotype.Component

@Component
class BlockEventConsumer(private val blockApplicationService: BlockApplicationService): EventConsumer {

    @Subscribe
    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val meeting = event.confirmedMeeting
        blockApplicationService.createBlock(
            meeting.initiatorId,
            CreateBlockCommand(meeting.slot.startTime, meeting.slot.endTime, meeting.meetingRecipient.email!!)
        )
    }
}