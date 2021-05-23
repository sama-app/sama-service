package com.sama.calendar.application

import com.sama.calendar.domain.InitiatedMeeting
import com.sama.calendar.domain.MeetingEntity
import com.sama.calendar.domain.MeetingId
import com.sama.calendar.domain.MeetingRepository
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class MeetingApplicationService(
    private val meetingRepository: MeetingRepository
) {

    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingId {
        val meetingId = meetingRepository.nextIdentity()
        val meeting = InitiatedMeeting(meetingId, userId, Duration.ofMinutes(command.duration), emptyList())

        val nextSlotIdentity = meetingRepository.nextSlotIdentity()

        MeetingEntity.new(meeting).also { meetingRepository.save(it) }

        return meetingId
    }

    fun addSuggestedSlot(userId: UserId, meetingId: MeetingId, command: AddSuggestSlotCommand): Boolean {
        TODO("not implemented")
    }

    fun modifySuggestedSlot(userId: UserId, meetingId: MeetingId, command: ModifySuggestSlotCommand): Boolean {
        TODO("not implemented")
    }

    fun removeSuggestedSlot(userId: UserId, meetingId: MeetingId, command: RemoveSuggestSlotCommand): Boolean {
        TODO("not implemented")
    }

    fun proposeMeeting(userId: UserId, meetingId: MeetingId, command: ProposeMeetingCommand): Boolean {
        TODO("not implemented")
    }

    fun confirmMeeting(command: ConfirmMeetingCommand): Boolean {
        TODO("not implemented")
    }
}

