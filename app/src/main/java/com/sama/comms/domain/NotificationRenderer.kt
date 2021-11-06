package com.sama.comms.domain

import java.time.ZonedDateTime

interface NotificationRenderer {
    fun renderMeetingInvitation(sender: CommsUser): Message
    fun renderMeetingProposed(sender: CommsUser, receiver: CommsUser, startDateTime: ZonedDateTime): Message
    fun renderMeetingConfirmed(senderDisplayName: String, receiver: CommsUser, startDateTime: ZonedDateTime): Message
    fun renderMeetingConfirmed(sender: CommsUser, receiver: CommsUser, startDateTime: ZonedDateTime) =
        renderMeetingConfirmed(sender.displayName, receiver, startDateTime)

    fun renderConnectionRequested(sender: CommsUser): Message
    fun renderConnectionRequestRejected(sender: CommsUser): Message
    fun renderUserConnected(sender: CommsUser): Message
}