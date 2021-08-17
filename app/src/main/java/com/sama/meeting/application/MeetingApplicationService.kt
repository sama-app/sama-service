package com.sama.meeting.application

import com.sama.calendar.application.CalendarEventConsumer
import com.sama.calendar.application.EventApplicationService
import com.sama.common.ApplicationService
import com.sama.common.NotFoundException
import com.sama.common.findByIdOrThrow
import com.sama.common.toMinutes
import com.sama.comms.application.CommsEventConsumer
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
import io.sentry.spring.tracing.SentryTransaction
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
    private val meetingInvitationView: MeetingInvitationView,
    private val meetingView: MeetingView,
    private val meetingCodeGenerator: MeetingCodeGenerator,
    private val eventApplicationService: EventApplicationService,
    private val userRepository: UserRepository,
    private val calendarEventConsumer: CalendarEventConsumer,
    private val commsEventConsumer: CommsEventConsumer,
    private val clock: Clock,
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
            suggestionSlotCount
        )
    }

    @Transactional
    @PreAuthorize("@auth.hasAccessByCode(#userId, #command.meetingIntentCode)")
    fun proposeMeeting(userId: UserId, command: ProposeMeetingCommand): MeetingInvitationDTO {
        val meetingIntentEntity = meetingIntentRepository.findByCodeOrThrow(command.meetingIntentCode)

        val meetingId = meetingRepository.nextIdentity()
        val meetingCode = meetingCodeGenerator.generate()

        val meetingIntent = MeetingIntent.of(meetingIntentEntity).getOrThrow()
        val proposedSlots = command.proposedSlots.map { it.toValueObject() }
        val proposedMeeting = meetingIntent
            .propose(meetingId, meetingCode, proposedSlots)
            .getOrThrow()

        MeetingEntity.new(proposedMeeting).also { meetingRepository.save(it) }

        return meetingInvitationView.render(proposedMeeting, meetingIntent.timezone)
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
        val blockingCalendarEvents = eventApplicationService.fetchEvents(
            proposedMeeting.initiatorId,
            start.toLocalDate(), end.toLocalDate(), start.zone
        )
        val availableSlots = proposedMeeting.availableSlots(blockingCalendarEvents.events, clock)

        return meetingView.render(proposedMeeting, availableSlots)
    }

    @Transactional
    fun confirmMeeting(userId: UserId?, meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean {
        val meetingEntity = meetingRepository.findByCodeOrThrow(meetingCode)
        val intentEntity = meetingIntentRepository.findByIdOrThrow(meetingEntity.meetingIntentId)

        val proposedMeeting = when (val meeting = meetingFrom(intentEntity, meetingEntity).getOrThrow()) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meetingCode)
            is ExpiredMeeting -> throw NotFoundException(ProposedMeeting::class, meetingCode)
            else -> throw InvalidMeetingStatusException(meetingCode, meeting.status)
        }

        val meetingRecipient = if (command.recipientEmail != null) {
            userRepository.findByEmail(command.recipientEmail)
                ?.let { MeetingRecipient.fromUser(it) }
                ?: MeetingRecipient.fromEmail(command.recipientEmail)
        } else {
            userRepository.findByIdOrThrow(userId!!)
                .let { MeetingRecipient.fromUser(it) }
        }

        val confirmedMeeting = proposedMeeting
            .confirm(command.slot.toValueObject(), meetingRecipient)
            .getOrThrow()

        meetingEntity.applyChanges(confirmedMeeting).also { meetingRepository.save(it) }

        // "manual" event publishing
        val event = MeetingConfirmedEvent(confirmedMeeting)
        calendarEventConsumer.onMeetingConfirmed(event)
        commsEventConsumer.onMeetingConfirmed(event)

        return true
    }

    @Transactional(readOnly = true)
    fun findProposedSlots(
        userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime,
    ): List<MeetingSlotDTO> {
        return meetingRepository.findAllProposedSlots(userId, startDateTime, endDateTime)
            .map { it.toDTO() }
    }

    @SentryTransaction(operation = "expireMeetings")
    @Scheduled(cron = "0 0/15 * * * *")
    @Transactional
    fun expireMeetings() {
        val expiringMeetingIds = meetingRepository.findAllIdsExpiring(ZonedDateTime.now(clock))
        if (expiringMeetingIds.isEmpty()) {
            return
        }
        meetingRepository.updateStatus(MeetingStatus.EXPIRED, expiringMeetingIds)
    }
}

