package com.sama.calendar.application

import com.sama.calendar.domain.*
import com.sama.common.NotFoundException
import com.sama.common.findByIdOrThrow
import com.sama.common.toMinutes
import com.sama.suggest.application.SlotSuggestionRequest
import com.sama.suggest.application.SlotSuggestionService
import com.sama.users.domain.UserId
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MeetingApplicationService(
    private val meetingRepository: MeetingRepository,
    private val slotSuggestionService: SlotSuggestionService
) {

    fun findMeeting(userId: UserId, meetingId: MeetingId): MeetingDTO {
        val meetingEntity = meetingRepository.findByIdOrThrow(meetingId)
        return meetingEntity.toDTO()
    }

    @Transactional
    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingDTO {
        val meetingId = meetingRepository.nextIdentity()

        val meetingDuration = command.duration.toMinutes()
        val meetingRecipient = command.recipientEmail?.let { MeetingRecipient.fromEmail(it) }

        val slotSuggestionRequest = SlotSuggestionRequest(command.suggestedSlotCount, meetingDuration)
        val suggestedSlots = slotSuggestionService.suggestSlots(userId, slotSuggestionRequest)
            .map {
                val slotId = meetingRepository.nextSlotIdentity()
                MeetingSlot.new(slotId, it.startTime, it.endTime)
            }

        val meeting = InitiatedMeeting(meetingId, userId, meetingDuration, suggestedSlots, meetingRecipient)

        val meetingEntity = MeetingEntity.new(meeting).also { meetingRepository.save(it) }
        return meetingEntity.toDTO()
    }

    @Transactional
    @PreAuthorize("@auth.hasAccess(#userId, #meetingId)")
    fun proposeMeeting(userId: UserId, meetingId: MeetingId, command: ProposeMeetingCommand): Boolean {
        val meetingEntity = meetingRepository.findByIdOrThrow(meetingId)

        val meetingCode = MeetingCodeGenerator.default().generate()
        val proposedMeeting = InitiatedMeeting.of(meetingEntity).getOrThrow()
            .propose(command.proposedSlots, meetingCode)
            .getOrThrow()

        meetingEntity.applyChanges(proposedMeeting).also { meetingRepository.save(it) }
        return true
    }

    @Transactional
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

