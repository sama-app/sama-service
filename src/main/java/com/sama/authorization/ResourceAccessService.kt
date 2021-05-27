package com.sama.authorization

import com.sama.calendar.domain.MeetingId
import com.sama.calendar.domain.MeetingRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

/**
 * Service to determine whether users have access to resources
 */
@Service("auth")
class ResourceAccessService(private val meetingRepository: MeetingRepository) {
    fun hasAccess(userId: UserId, meetingId: MeetingId): Boolean {
        return meetingRepository.existsByIdAndInitiatorId(meetingId, userId)
    }
}
