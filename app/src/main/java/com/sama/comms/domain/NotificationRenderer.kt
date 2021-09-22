package com.sama.comms.domain

import java.time.ZonedDateTime

interface NotificationRenderer {
    fun renderMeetingInvitation(sender: CommsUser): Notification
    fun renderMeetingProposed(sender: CommsUser, receiver: CommsUser, startDateTime: ZonedDateTime): Notification
    fun renderMeetingConfirmed(senderDisplayName: String, receiver: CommsUser, startDateTime: ZonedDateTime): Notification
    fun renderMeetingConfirmed(sender: CommsUser, receiver: CommsUser, startDateTime: ZonedDateTime) =
        renderMeetingConfirmed(sender.displayName, receiver, startDateTime)

    fun renderConnectionRequested(sender: CommsUser): Notification
    fun renderConnectionRequestRejected(sender: CommsUser): Notification
    fun renderUserConnected(sender: CommsUser): Notification
}