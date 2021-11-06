package com.sama.comms.application

import com.sama.comms.domain.CommsUser
import com.sama.comms.domain.CommsUserRepository
import com.sama.comms.domain.Message
import com.sama.comms.domain.NotificationRenderer
import com.sama.comms.domain.NotificationSender
import com.sama.connection.domain.UserConnectedEvent
import com.sama.connection.domain.UserConnectionRequestCreatedEvent
import com.sama.connection.domain.UserConnectionRequestRejectedEvent
import com.sama.integration.google.calendar.domain.CalendarDatesUpdatedEvent
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.meeting.domain.MeetingProposedEvent
import com.sama.meeting.domain.NewMeetingSlotsProposedEvent
import com.sama.meeting.domain.SamaSamaProposedMeeting
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
        if (meeting !is SamaSamaProposedMeeting) {
            return
        }

        val receiver = receiverCommsUser(event.actorId, meeting.initiatorId, meeting.recipientId)
            ?: return
        val sender = commsUserRepository.find(event.actorId)

        val notification = when {
            meeting.isInvitation -> notificationRenderer.renderMeetingInvitation(sender)
            else -> {
                val slotDateTime = meeting.proposedSlots[0].startDateTime
                notificationRenderer.renderMeetingProposed(sender, receiver, slotDateTime)
            }
        }

        notificationSender.send(receiver.userId, notification)
    }

    fun onNewMeetingSlotsProposed(event: NewMeetingSlotsProposedEvent) {
        val meeting = event.proposedMeeting

        val receiver = receiverCommsUser(event.actorId, meeting.initiatorId, meeting.recipientId)
            ?: return
        val sender = commsUserRepository.find(event.actorId)

        val slotDateTime = meeting.proposedSlots[0].startDateTime
        val notification = notificationRenderer.renderMeetingProposed(sender, receiver, slotDateTime)

        notificationSender.send(receiver.userId, notification)
    }

    fun onMeetingConfirmed(event: MeetingConfirmedEvent) {
        val confirmedMeeting = event.confirmedMeeting
        val slotDateTime = confirmedMeeting.slot.startDateTime
        val meetingRecipient = confirmedMeeting.recipient

        if (meetingRecipient is EmailRecipient) {
            if (confirmedMeeting.initiatorId == event.actorId) {
                return
            }
            val receiver = commsUserRepository.find(confirmedMeeting.initiatorId)
            val notification = notificationRenderer.renderMeetingConfirmed(meetingRecipient.email, receiver, slotDateTime)

            notificationSender.send(receiver.userId, notification)
        } else if (meetingRecipient is UserRecipient) {
            val receiver = receiverCommsUser(event.actorId, confirmedMeeting.initiatorId, meetingRecipient.recipientId)
                ?: return
            val sender = senderCommsUser(event.actorId, confirmedMeeting.initiatorId, meetingRecipient.recipientId)
                ?: return
            if (sender.userId == receiver.userId) {
                return
            }

            val notification = notificationRenderer.renderMeetingConfirmed(sender, receiver, slotDateTime)
            notificationSender.send(receiver.userId, notification)
        }
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

    fun onCalendarDatesUpdated(event: CalendarDatesUpdatedEvent) {
        notificationSender.send(
            event.userId,
            Message(
                additionalData = mapOf(
                    "event_type" to "calendar_dates_updated",
                    "dates" to event.dates.joinToString(separator = ",")
                )
            )
        )
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

    private fun senderCommsUser(actorId: UserId?, initiatorId: UserId, recipientId: UserId): CommsUser? {
        return when (actorId) {
            null -> recipientId
            initiatorId -> initiatorId
            recipientId -> recipientId
            else -> {
                logger.warn("Could not determine comms recipient id")
                null
            }
        }?.let { commsUserRepository.find(it) }
    }
}
