package com.sama.meeting.application

import com.sama.common.ApplicationService
import com.sama.meeting.domain.MeetingRepository
import com.sama.users.domain.UserId
import java.time.ZonedDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class MeetingDataService(private val meetingRepository: MeetingRepository) {

    @Transactional(readOnly = true)
    fun findProposedSlots(userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime) =
        meetingRepository.findAllProposedSlots(userId, startDateTime, endDateTime)
            .map { it.toDTO() }
}

