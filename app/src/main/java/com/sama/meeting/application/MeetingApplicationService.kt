package com.sama.meeting.application

import com.sama.calendar.application.BlockEventConsumer
import com.sama.calendar.domain.BlockRepository
import com.sama.common.ApplicationService
import com.sama.common.NotFoundException
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
import com.sama.users.domain.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime

@ApplicationService
@Service
class MeetingApplicationService(
    private val meetingIntentRepository: MeetingIntentRepository,
    private val meetingRepository: MeetingRepository,
    private val slotSuggestionService: SlotSuggestionService,
    private val meetingInvitationService: MeetingInvitationService,
    private val meetingCodeGenerator: MeetingCodeGenerator,
    private val userRepository: UserRepository,
    private val blockRepository: BlockRepository,
    private val blockEventConsumer: BlockEventConsumer,
    private val clock: Clock
) {

    @Transactional
    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingIntentDTO {
        val meetingId = meetingIntentRepository.nextIdentity()

        val request = command.toSlotSuggestionRequest()
        val suggestedSlots = slotSuggestionService.suggestSlots(userId, request).suggestions
            .map { MeetingSlot(it.startDateTime, it.endDateTime) }

        val meetingIntent = MeetingIntent(
            meetingId,
            userId,
            command.durationMinutes.toMinutes(),
            command.timeZone,
            suggestedSlots
        )

        val entity = MeetingIntentEntity.new(meetingIntent).also { meetingIntentRepository.save(it) }
        return entity.toDTO()
    }

    private fun InitiateMeetingCommand.toSlotSuggestionRequest(): SlotSuggestionRequest {
        return SlotSuggestionRequest(
            durationMinutes.toMinutes(),
            timeZone,
            suggestionSlotCount,
            suggestionDayCount,
        )
    }

    @Transactional
    @PreAuthorize("@auth.hasAccessByCode(#userId, #command.meetingIntentCode)")
    fun proposeMeeting(userId: UserId, command: ProposeMeetingCommandV2): MeetingInvitationDTO {
        val meetingIntentEntity = meetingIntentRepository.findByCodeOrThrow(command.meetingIntentCode)
        val proposedSlots = command.proposedSlots.map { it.toValueObject() }
        return proposeMeeting(meetingIntentEntity, proposedSlots)
    }

    @Transactional
    @PreAuthorize("@auth.hasAccess(#userId, #meetingIntentId)")
    fun proposeMeeting(
        userId: UserId,
        meetingIntentId: MeetingIntentId,
        command: ProposeMeetingCommand
    ): MeetingInvitationDTO {
        val meetingIntentEntity = meetingIntentRepository.findByIdOrThrow(meetingIntentId)
        val proposedSlots = command.proposedSlots.map { it.toValueObject() }
        return proposeMeeting(meetingIntentEntity, proposedSlots)
    }

    private fun proposeMeeting(
        meetingIntentEntity: MeetingIntentEntity,
        proposedSlots: List<MeetingSlot>
    ): MeetingInvitationDTO {
        val meetingId = meetingRepository.nextIdentity()

        val meetingCode = meetingCodeGenerator.generate()

        val meetingIntent = MeetingIntent.of(meetingIntentEntity).getOrThrow()
        val proposedMeeting = meetingIntent
            .propose(meetingId, meetingCode, proposedSlots)
            .getOrThrow()

        val meetingInvitation = meetingInvitationService.findForProposedMeeting(proposedMeeting, meetingIntent.timezone)
        val initiatorEntity = userRepository.findByIdOrThrow(proposedMeeting.initiatorId)

        MeetingEntity.new(proposedMeeting).also { meetingRepository.save(it) }

        return MeetingInvitationDTO(
            ProposedMeetingDTO(
                proposedMeeting.proposedSlots.map { it.toDTO() },
                initiatorEntity.toInitiatorDTO()
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
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meetingCode)
            is ExpiredMeeting -> throw NotFoundException(ProposedMeeting::class, meetingCode)
            else -> throw InvalidMeetingStatusException(meetingCode, meeting.status)
        }

        val (start, end) = proposedMeeting.proposedSlotsRange()
        val blocks = blockRepository.findAll(proposedMeeting.initiatorId, start, end)
        val availableProposedSlots = proposedMeeting.availableProposedSlots(blocks, clock)

        val initiatorEntity = userRepository.findByIdOrThrow(proposedMeeting.initiatorId)

        return ProposedMeetingDTO(
            availableProposedSlots.map { it.toDTO() },
            initiatorEntity.toInitiatorDTO()
        )
    }

    @Transactional
    fun confirmMeeting(meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean {
        val meetingEntity = meetingRepository.findByCodeOrThrow(meetingCode)
        val intentEntity = meetingIntentRepository.findByIdOrThrow(meetingEntity.meetingIntentId)

        val proposedMeeting = when (val meeting = meetingFrom(intentEntity, meetingEntity).getOrThrow()) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meetingCode)
            is ExpiredMeeting -> throw NotFoundException(ProposedMeeting::class, meetingCode)
            else -> throw InvalidMeetingStatusException(meetingCode, meeting.status)
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

    @Scheduled(cron = "0 0/15 * * * *")
    @Transactional
    fun expireMeetings() {
        val expiringMeetingIds = meetingRepository.findAllIdsExpiring(ZonedDateTime.now())
        if (expiringMeetingIds.isEmpty()) {
            return
        }
        meetingRepository.updateStatus(MeetingStatus.EXPIRED, expiringMeetingIds)
    }
}

