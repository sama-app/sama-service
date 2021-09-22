package com.sama.comms.application

import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.CommsUserRepository
import com.sama.comms.domain.NotificationRenderer
import com.sama.comms.domain.NotificationSender
import com.sama.connection.domain.UserConnectedEvent
import com.sama.connection.domain.UserConnectionRequestCreatedEvent
import com.sama.connection.domain.UserConnectionRequestRejectedEvent
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.meeting.domain.MeetingProposedEvent
import com.sama.meeting.domain.NewMeetingSlotsProposedEvent
import com.sama.meeting.domain.UserRecipient
import com.sama.users.domain.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CommsEventConsumer(
    private val commsUserRepository: CommsUserRepository,
    private val notificationRenderer: NotificationRenderer,
    private val notificationSender: NotificationSender,
) {
    private var logger: Logger = LoggerFactory.getLogger(CommsEventConsumer::class.java)

    fun onMeetingProposed(event: MeetingProposedEvent) {
        val meeting = event.proposedMeeting
        if (!meeting.isSamaToSama()) {
            return
        }

        val receiver = receiverCommsUser(event.actorId, meeting.initiatorId, meeting.recipientId)
            ?: return
        val sender = commsUserRepository.find(event.actorId)

        val notification = when {
            meeting.isInvitation() -> notificationRenderer.renderMeetingInvitation(sender)
            else -> {
                val slotDateTime = meeting.proposedSlots[0].startDateTime
                notificationRenderer.renderMeetingProposed(sender, receiver, slotDateTime)
            }
        }

        notificationSender.send(receiver.userId, notification)
    }

    fun onNewMeetingSlotsProposed(event: NewMeetingSlotsProposedEvent) {
        val meeting = event.proposedMeeting
        if (!meeting.isSamaToSama()) {
            return
        }

        val receiver = receiverCommsUser(event.actorId, meeting.initiatorId, meeting.recipientId)
            ?: return
        val sender = commsUserRepository.find(event.actorId)

        val slotDateTime = meeting.proposedSlots[0].startDateTime
        val notification = notificationRenderer.renderMeetingProposed(sender, receiver, slotDateTime)

        notificationSender.send(receiver.userId, notification)
    }

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val confirmedMeeting = event.confirmedMeeting
        val (recipientEmail, recipientId) = when (val recipient = confirmedMeeting.meetingRecipient) {
            is EmailRecipient -> recipient.email to null
            is UserRecipient -> recipient.email to recipient.recipientId
        }

        val receiver = receiverCommsUser(event.actorId, confirmedMeeting.initiatorId, recipientId)
            ?: return
        val sender = event.actorId?.let { commsUserRepository.find(it) }

        val slotDateTime = confirmedMeeting.slot.startDateTime
        val notification = when (sender) {
            null -> notificationRenderer.renderMeetingConfirmed(recipientEmail, receiver, slotDateTime)
            else -> notificationRenderer.renderMeetingConfirmed(sender, receiver, slotDateTime)
        }

        notificationSender.send(receiver.userId, notification)
    }

    fun onConnectionRequestCreated(event: UserConnectionRequestCreatedEvent) {
        val connectionRequest = event.connectionRequest

        val sender = commsUserRepository.find(connectionRequest.initiatorUserId)
        val receiver = commsUserRepository.find(connectionRequest.recipientUserId)

        val notification = notificationRenderer.renderConnectionRequested(sender)
        notificationSender.send(receiver.userId, notification)
    }

    fun onUserConnected(event: UserConnectedEvent) {
        val userConnection = event.userConnection

        val sender = commsUserRepository.find(event.actorId)
        val receiver = receiverCommsUser(event.actorId, userConnection.leftUserId, userConnection.rightUserId)
            ?: return

        val notification = notificationRenderer.renderUserConnected(sender)
        notificationSender.send(receiver.userId, notification)
    }

    fun onConnectionRequestRejected(event: UserConnectionRequestRejectedEvent) {
        val connectionRequest = event.connectionRequest

        val sender = commsUserRepository.find(connectionRequest.recipientUserId)
        val receiver = commsUserRepository.find(connectionRequest.initiatorUserId)

        val notification = notificationRenderer.renderConnectionRequestRejected(sender)
        notificationSender.send(receiver.userId, notification)
    }

    private fun receiverCommsUser(actorId: UserId?, initiatorId: UserId, recipientId: UserId?): CommsUser? {
        return when {
            actorId == null || recipientId == null -> initiatorId
            actorId == initiatorId -> recipientId
            actorId == recipientId -> initiatorId
            else -> {
                logger.warn("Could not determine comms recipient id")
                null
            }
        }?.let { commsUserRepository.find(it) }
    }
}
