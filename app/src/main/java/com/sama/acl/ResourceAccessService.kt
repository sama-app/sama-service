package com.sama.acl

import com.sama.connection.domain.ConnectionRequestId
import com.sama.connection.domain.ConnectionRequestRepository
import com.sama.meeting.domain.MeetingIntentCode
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.infrastructure.jpa.MeetingIntentJpaRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

/**
 * Service to determine whether users have access to resources
 */
@Service("auth")
class ResourceAccessService(
    private val meetingIntentRepository: MeetingIntentJpaRepository,
    private val connectionRequestRepository: ConnectionRequestRepository
) {
    fun hasAccess(userId: UserId, meetingIntentId: MeetingIntentId): Boolean {
        return meetingIntentRepository.existsByIdAndInitiatorId(meetingIntentId, userId)
    }

    fun hasAccessByCode(userId: UserId, meetingIntentCode: MeetingIntentCode): Boolean {
        return meetingIntentRepository.existsByCodeAndInitiatorId(meetingIntentCode, userId)
    }

    fun hasRecipientAccess(userId: UserId, connectionRequestId: ConnectionRequestId): Boolean {
        return connectionRequestRepository.findByIdOrThrow(connectionRequestId).recipientUserId == userId
    }
}
