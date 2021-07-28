package com.sama.comms.application

import com.sama.common.ApplicationService
import com.sama.comms.domain.CommsUserRepository
import com.sama.comms.domain.NotificationRenderer
import com.sama.comms.domain.NotificationSender
import org.springframework.stereotype.Service

@ApplicationService
@Service
class MeetingCommsApplicationService(
    private val commsUserRepository: CommsUserRepository,
    private val notificationRenderer: NotificationRenderer,
    private val notificationSender: NotificationSender
) {

    fun sendMeetingConfirmedComms(command: SendMeetingConfirmedCommand) {
        val commsUser = commsUserRepository.findById(command.userId)

        val notification = notificationRenderer.renderMeetingConfirmed(
            commsUser,
            command.meetingAttendeeEmail,
            command.meetingStartDateTime
        )

        notificationSender.send(commsUser.userId, notification)
    }
}