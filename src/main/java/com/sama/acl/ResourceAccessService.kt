package com.sama.acl

import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingIntentRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

/**
 * Service to determine whether users have access to resources
 */
@Service("auth")
class ResourceAccessService(private val meetingIntentRepository: MeetingIntentRepository) {
    fun hasAccess(userId: UserId, meetingIntentId: MeetingIntentId): Boolean {
        return meetingIntentRepository.existsByIdAndInitiatorId(meetingIntentId, userId)
    }
}
