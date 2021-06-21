package com.sama.meeting.application

import com.sama.calendar.application.BlockEventConsumer
import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.common.findByIdOrThrow
import com.sama.common.toMinutes
import com.sama.meeting.domain.*
import com.sama.meeting.domain.aggregates.MeetingEntity
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.repositories.MeetingCodeGenerator
import com.sama.meeting.domain.repositories.MeetingIntentRepository
import com.sama.meeting.domain.repositories.MeetingRepository
import com.sama.meeting.domain.repositories.findByCodeOrThrow
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.users.domain.UserId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@ApplicationService
@Service
class MeetingApplicationService(
    private val meetingIntentRepository: MeetingIntentRepository,
    private val meetingRepository: MeetingRepository,
    private val slotSuggestionService: SlotSuggestionService,
    private val meetingInvitationService: MeetingInvitationService,
    private val meetingCodeGenerator: MeetingCodeGenerator,
    private val blockRepository: BlockRepository,
    private val blockEventConsumer: BlockEventConsumer,
    private val clock: Clock
) {

    @Transactional
    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingIntentDTO {
        val meetingId = meetingIntentRepository.nextIdentity()

        val suggestedSlots = when (command.suggestionSlotCount) {
            0 -> emptyList()
            else -> {
                val request = command.toSlotSuggestionRequest()
                slotSuggestionService.suggestSlots(userId, request).suggestions
                    .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            }
        }

        val meeting = MeetingIntent(
            meetingId,
            userId,
            command.durationMinutes.toMinutes(),
            command.timeZone,
            suggestedSlots
        )

        val meetingEntity = MeetingIntentEntity.new(meeting).also { meetingIntentRepository.save(it) }
        return meetingEntity.toDTO()
    }

    private fun InitiateMeetingCommand.toSlotSuggestionRequest(): SlotSuggestionRequest {
        return SlotSuggestionRequest(
            durationMinutes.toMinutes(),
            timeZone,
            suggestionSlotCount,
            LocalDateTime.now(clock),
            LocalDateTime.now(clock).plusDays(suggestionDayCount.toLong())
        )
    }

    @Transactional(readOnly = true)
    fun findMeetingIntent(userId: UserId, meetingIntentId: MeetingIntentId): MeetingIntentDTO {
        val meetingEntity = meetingIntentRepository.findByIdOrThrow(meetingIntentId)
        return meetingEntity.toDTO()
    }

    @Transactional
    @PreAuthorize("@auth.hasAccess(#userId, #meetingIntentId)")
    fun proposeMeeting(
        userId: UserId,
        meetingIntentId: MeetingIntentId,
        command: ProposeMeetingCommand
    ): MeetingInvitationDTO {
        val meetingEntity = meetingIntentRepository.findByIdOrThrow(meetingIntentId)
        val meetingId = meetingRepository.nextIdentity()

        val meetingCode = meetingCodeGenerator.generate()
        val proposedSlots = command.proposedSlots.map { it.toValueObject() }

        val proposedMeeting = MeetingIntent.of(meetingEntity).getOrThrow()
            .propose(meetingId, meetingCode, proposedSlots)
            .getOrThrow()

        val meetingInvitation = meetingInvitationService.findForProposedMeeting(proposedMeeting)

        MeetingEntity.new(proposedMeeting).also { meetingRepository.save(it) }

        return MeetingInvitationDTO(
            ProposedMeetingDTO(
                proposedMeeting.meetingId,
                proposedMeeting.proposedSlots.map { it.toDTO() },
                proposedMeeting.meetingCode,
            ),
            meetingInvitation.url,
            meetingInvitation.message
        )
    }


    @Transactional(readOnly = true)
    fun loadMeetingProposalFromCode(meetingCode: MeetingCode): ProposedMeetingDTO {
        val meetingEntity = meetingRepository.findByCodeOrThrow(meetingCode)
        val intentEntity = meetingIntentRepository.findByIdOrThrow(meetingEntity.meetingIntentId)

        val proposedMeeting = when (val meeting = meetingFrom(intentEntity, meetingEntity).getOrThrow()) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meeting.meetingId)
            is ExpiredMeeting -> throw  MeetingProposalExpiredException(meeting.meetingId)
            else -> throw InvalidMeetingStatusException(meeting.meetingId, meeting.status)
        }

        val (start, end) = proposedMeeting.proposedSlotsRange()
        val blocks = blockRepository.findAll(proposedMeeting.initiatorId, start, end)
        val availableProposedSlots = proposedMeeting.availableProposedSlots(blocks, clock)

        return ProposedMeetingDTO(
            proposedMeeting.meetingId,
            availableProposedSlots.map { it.toDTO() },
            meetingCode
        )
    }

    @Transactional
    fun confirmMeeting(meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean {
        val meetingEntity = meetingRepository.findByCodeOrThrow(meetingCode)
        val intentEntity = meetingIntentRepository.findByIdOrThrow(meetingEntity.meetingIntentId)

        val proposedMeeting = when (val meeting = meetingFrom(intentEntity, meetingEntity).getOrThrow()) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meeting.meetingId)
            is ExpiredMeeting -> throw  MeetingProposalExpiredException(meeting.meetingId)
            else -> throw InvalidMeetingStatusException(meeting.meetingId, meeting.status)
        }

        val meetingRecipient = command.recipientEmail.let { MeetingRecipient.fromEmail(it) }
        val confirmedMeeting = proposedMeeting
            .confirm(command.slot.toValueObject(), meetingRecipient)
            .getOrThrow()

        // "manual" event publishing
        blockEventConsumer.onMeetingConfirmed(MeetingConfirmedEvent(confirmedMeeting))

        meetingEntity.applyChanges(confirmedMeeting).also { meetingRepository.save(it) }
        return true
    }
}

