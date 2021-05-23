package com.sama.calendar.application

import com.sama.calendar.domain.MeetingId
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class MeetingApplicationService {

    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingDTO {
        TODO("not implemented")
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

