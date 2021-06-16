package com.sama.meeting.application

import com.sama.calendar.domain.BlockRepository
import com.sama.common.findByIdOrThrow
import com.sama.common.toMinutes
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.*
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.aggregates.MeetingProposalEntity
import com.sama.meeting.domain.repositories.MeetingIntentRepository
import com.sama.meeting.domain.repositories.MeetingProposalRepository
import com.sama.meeting.domain.repositories.findByCodeOrThrow
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.users.domain.UserId
import liquibase.pro.packaged.it
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import java.time.Clock
import java.time.LocalDateTime

@Service
class MeetingApplicationService(
    private val meetingIntentRepository: MeetingIntentRepository,
    private val meetingProposalRepository: MeetingProposalRepository,
    private val slotSuggestionService: SlotSuggestionService,
    private val blockRepository: BlockRepository,
    private val meetingUrlConfiguration: MeetingUrlConfiguration,
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
                    .map { MeetingSlot(it.startTime, it.endTime) }
            }
        }

        val meeting = MeetingIntent(
            meetingId,
            userId,
            null,
            command.duration.toMinutes(),
            command.timezone,
            suggestedSlots
        )

        val meetingEntity = MeetingIntentEntity.new(meeting).also { meetingIntentRepository.save(it) }
        return meetingEntity.toDTO()
    }

    private fun InitiateMeetingCommand.toSlotSuggestionRequest(): SlotSuggestionRequest {
        return SlotSuggestionRequest(
            duration.toMinutes(),
            timezone,
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
    ): MeetingProposalDTO {
        val meetingEntity = meetingIntentRepository.findByIdOrThrow(meetingIntentId)
        val meetingProposalId = meetingProposalRepository.nextIdentity()

        val meetingCode = MeetingCodeGenerator(meetingUrlConfiguration.codeLength).generate()
        val proposedSlots = command.proposedSlots.map { it.toValueObject() }

        val meetingProposal = MeetingIntent.of(meetingEntity).getOrThrow()
            .propose(meetingProposalId, meetingCode, proposedSlots)
            .getOrThrow()

        MeetingProposalEntity.new(meetingProposal).also { meetingProposalRepository.save(it) }

        return MeetingProposalDTO(
            meetingIntentId,
            meetingProposalId,
            proposedSlots.map { it.toDTO() },
            meetingCode,
            meetingCode.toUrl(meetingUrlConfiguration)
        )
    }

    @Transactional(readOnly = true)
    fun loadMeetingProposalFromCode(meetingCode: MeetingCode): MeetingProposalDTO {
        val proposalEntity = meetingProposalRepository.findByCodeOrThrow(meetingCode)
        val intentEntity = meetingIntentRepository.findByIdOrThrow(proposalEntity.meetingIntentId)

        val proposedMeeting = when (val meeting = meetingFrom(intentEntity, proposalEntity).getOrThrow()) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meeting.meetingProposalId)
            is ExpiredMeeting -> throw  MeetingProposalExpiredException(meeting.meetingProposalId)
            else -> throw InvalidMeetingStatusException(meeting.meetingProposalId, meeting.status)
        }

        val (start, end) = proposedMeeting.proposedSlotsRange()
        val blocks = blockRepository.findAll(proposedMeeting.initiatorId, start, end)
        val availableProposedSlots = proposedMeeting.availableProposedSlots(blocks, clock)

        return MeetingProposalDTO(
            proposedMeeting.meetingIntentId,
            proposedMeeting.meetingProposalId,
            availableProposedSlots.map { it.toDTO() },
            proposedMeeting.meetingCode,
            proposedMeeting.meetingCode.toUrl(meetingUrlConfiguration)
        )
    }

    @Transactional
    fun confirmMeeting(meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean {
        val proposalEntity = meetingProposalRepository.findByCodeOrThrow(meetingCode)
        val intentEntity = meetingIntentRepository.findByIdOrThrow(proposalEntity.meetingIntentId)

        val proposedMeeting = when (val meeting = meetingFrom(intentEntity, proposalEntity).getOrThrow()) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meeting.meetingProposalId)
            is ExpiredMeeting -> throw  MeetingProposalExpiredException(meeting.meetingProposalId)
            else -> throw InvalidMeetingStatusException(meeting.meetingProposalId, meeting.status)
        }

        val meetingRecipient = command.recipientEmail.let { MeetingRecipient.fromEmail(it) }
        val confirmedMeeting = proposedMeeting
            .confirm(command.slot.toValueObject(), meetingRecipient)
            .getOrThrow()

        proposalEntity.applyChanges(confirmedMeeting).also { meetingProposalRepository.save(it) }
        return true
    }
}

