package com.sama.meeting.application

import com.sama.users.domain.UserDeletedEvent
import org.springframework.stereotype.Component

@Component
class MeetingEventConsumer(private val meetingApplicationService: MeetingApplicationService) {

    fun onUserDeleted(event: UserDeletedEvent) {
        // TODO
    }
}