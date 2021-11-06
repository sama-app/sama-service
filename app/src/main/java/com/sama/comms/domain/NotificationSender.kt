package com.sama.comms.domain

import com.sama.users.domain.UserId

interface NotificationSender {
    fun send(receiverUserId: UserId, message: Message)
}