package com.sama.calendar.application

import com.sama.calendar.domain.*
import com.sama.common.NotFoundException
import com.sama.common.findByIdOrThrow
import com.sama.common.toMinutes
import com.sama.suggest.application.SlotSuggestionRequest
import com.sama.suggest.application.SlotSuggestionService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service

@Service
class MeetingApplicationService(
    private val meetingRepository: MeetingRepository,
    private val slotSuggestionService: SlotSuggestionService
) {

    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingId {
        val meetingId = meetingRepository.nextIdentity()

        val meetingDuration = command.duration.toMinutes()
        val suggestedSlots = slotSuggestionService.suggestSlots(userId, SlotSuggestionRequest(10, meetingDuration))
            .map {
                val slotId = meetingRepository.nextSlotIdentity()
                MeetingSlot.new(slotId, it.startTime, it.endTime)
            }

        val meetingRecipient = command.recipientEmail?.let { MeetingRecipient.fromEmail(it) }
        val meeting = InitiatedMeeting(meetingId, userId, meetingDuration, suggestedSlots, meetingRecipient)

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
        val meetingEntity = meetingRepository.findByIdOrThrow(meetingId)

        val meetingCode = MeetingCodeGenerator.default().generate()
        val proposedMeeting = InitiatedMeeting.of(meetingEntity).getOrThrow()
            .propose(command.proposedSlots, meetingCode)
            .getOrThrow()

        meetingEntity.applyChanges(proposedMeeting).also { meetingRepository.save(it) }
        return true
    }

    fun confirmMeeting(userId: UserId, command: ConfirmMeetingCommand): Boolean {
        val meetingEntity = meetingRepository.findByCode(command.meetingCode)
            ?: throw NotFoundException(MeetingEntity::class, command.meetingCode)

        val meetingRecipient = command.recipientEmail.let { MeetingRecipient.fromEmail(it) }
        val confirmedMeeting = ProposedMeeting.of(meetingEntity).getOrThrow()
            .confirm(command.slotId, meetingRecipient)
            .getOrThrow()

        meetingEntity.applyChanges(confirmedMeeting).also { meetingRepository.save(it) }
        return true
    }
}

