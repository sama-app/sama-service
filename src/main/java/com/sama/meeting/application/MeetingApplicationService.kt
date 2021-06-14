package com.sama.meeting.application

import com.sama.common.NotFoundException
import com.sama.common.findByIdOrThrow
import com.sama.common.toMinutes
import com.sama.meeting.domain.*
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.users.domain.UserId
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
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun findMeeting(userId: UserId, meetingIntentId: MeetingIntentId): MeetingIntentDTO {
        val meetingEntity = meetingIntentRepository.findByIdOrThrow(meetingIntentId)
        return meetingEntity.toDTO()
    }

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

    @Transactional
    @PreAuthorize("@auth.hasAccess(#userId, #meetingIntentId)")
    fun proposeMeeting(
        userId: UserId,
        meetingIntentId: MeetingIntentId,
        command: ProposeMeetingCommand
    ): MeetingProposalDTO {
        val meetingEntity = meetingIntentRepository.findByIdOrThrow(meetingIntentId)
        val meetingProposalId = meetingProposalRepository.nextIdentity()

        val meetingCode = MeetingCodeGenerator.default().generate()
        val proposedSlots = command.proposedSlots.map { it.toValueObject() }

        val meetingProposal = MeetingIntent.of(meetingEntity).getOrThrow()
            .propose(meetingProposalId, meetingCode, proposedSlots)
            .getOrThrow()

        MeetingProposalEntity.new(meetingProposal).also { meetingProposalRepository.save(it) }

        return MeetingProposalDTO(meetingIntentId, meetingProposalId, meetingCode, meetingCode.toUrl())
    }

    // TODO: properly configure
    fun MeetingCode.toUrl(): String {
        return UriComponentsBuilder.newInstance()
            .scheme("https")
            .host("app.yoursama.com")
            .path("/$this")
            .build().toUriString()
    }

    @Transactional
    fun confirmMeeting(userId: UserId, command: ConfirmMeetingCommand): Boolean {
        val proposalEntity = meetingProposalRepository.findByCodeAndStatus(
            command.meetingCode,
            MeetingProposalStatus.PROPOSED
        )
            ?: throw NotFoundException(MeetingProposalEntity::class, "code", command.meetingCode)
        val intentEntity = meetingIntentRepository.findByIdOrThrow(proposalEntity.meetingIntentId)

        val meetingRecipient = command.recipientEmail.let { MeetingRecipient.fromEmail(it) }
        val confirmedMeeting = MeetingProposal.of(intentEntity, proposalEntity).getOrThrow()
            .confirm(command.slot.toValueObject(), meetingRecipient)
            .getOrThrow()

        proposalEntity.applyChanges(confirmedMeeting).also { meetingProposalRepository.save(it) }
        return true
    }
}

