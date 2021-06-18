package com.sama.meeting.application

import com.google.common.eventbus.Subscribe
import com.sama.calendar.application.BlockApplicationService
import com.sama.calendar.application.CreateBlockCommand
import com.sama.events.EventConsumer
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.users.domain.UserDeletedEvent
import org.springframework.stereotype.Component

@Component
class MeetingEventConsumer(private val meetingApplicationService: MeetingApplicationService) : EventConsumer {

    @Subscribe
    fun onUserDeleted(event: UserDeletedEvent) {
        // TODO
    }
}