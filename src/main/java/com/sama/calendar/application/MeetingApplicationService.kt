package com.sama.calendar.application

import com.sama.calendar.domain.*
import com.sama.calendar.domain.MeetingProposalStatus.PROPOSED
import com.sama.common.NotFoundException
import com.sama.common.findByIdOrThrow
import com.sama.common.toMinutes
import com.sama.suggest.application.SlotSuggestionRequest
import com.sama.suggest.application.SlotSuggestionService
import com.sama.users.domain.UserId
import liquibase.pro.packaged.it
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder

@Service
class MeetingApplicationService(
    private val meetingIntentRepository: MeetingIntentRepository,
    private val meetingProposalRepository: MeetingProposalRepository,
    private val slotSuggestionService: SlotSuggestionService
) {

    fun findMeeting(userId: UserId, meetingIntentId: MeetingIntentId): MeetingIntentDTO {
        val meetingEntity = meetingIntentRepository.findByIdOrThrow(meetingIntentId)
        return meetingEntity.toDTO()
    }

    @Transactional
    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingIntentDTO {
        val meetingId = meetingIntentRepository.nextIdentity()

        val meetingDuration = command.duration.toMinutes()

        val slotSuggestionRequest = SlotSuggestionRequest(command.suggestedSlotCount, meetingDuration)
        val suggestedSlots = slotSuggestionService.suggestSlots(userId, slotSuggestionRequest)
            .map { MeetingSlot(it.startTime, it.endTime) }

        val meeting = MeetingIntent(meetingId, userId, null, meetingDuration, suggestedSlots)

        val meetingEntity = MeetingIntentEntity.new(meeting).also { meetingIntentRepository.save(it) }
        return meetingEntity.toDTO()
    }

    @Transactional
    @PreAuthorize("@auth.hasAccess(#userId, #meetingIntentId)")
    fun proposeMeeting(userId: UserId, meetingIntentId: MeetingIntentId, command: ProposeMeetingCommand): MeetingProposalDTO {
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
        val proposalEntity = meetingProposalRepository.findByCodeAndStatus(command.meetingCode, PROPOSED)
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

