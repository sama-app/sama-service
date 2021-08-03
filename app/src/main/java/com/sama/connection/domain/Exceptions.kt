package com.sama.connection.domain

import com.sama.common.DomainIntegrityException
import com.sama.common.DomainValidationException
import com.sama.users.domain.UserId

class InvalidConnectionRequest(val connectionRequestId: ConnectionRequestId) :
    DomainValidationException("Invalid ConnectionRequest#$connectionRequestId")

class UserAlreadyConnectedException(val initiatorId: UserId, val recipientId: UserId) :
    DomainIntegrityException("user_already_connected", "User#$initiatorId is already connected to User#$recipientId")