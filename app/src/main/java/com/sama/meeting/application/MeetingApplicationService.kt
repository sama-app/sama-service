package com.sama.meeting.application

import com.sama.calendar.application.CalendarEventConsumer
import com.sama.calendar.application.EventApplicationService
import com.sama.common.ApplicationService
import com.sama.common.NotFoundException
import com.sama.common.toMinutes
import com.sama.comms.application.CommsEventConsumer
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.ExpiredMeeting
import com.sama.meeting.domain.InvalidMeetingStatusException
import com.sama.meeting.domain.MeetingAlreadyConfirmedException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingCodeGenerator
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingIntentRepository
import com.sama.meeting.domain.MeetingRepository
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.UserRecipient
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.users.application.InternalUserService
import com.sama.users.domain.UserId
import io.sentry.spring.tracing.SentryTransaction
import java.time.Clock
import java.time.ZonedDateTime
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    private val userService: InternalUserService,
    private val calendarEventConsumer: CalendarEventConsumer,
    private val commsEventConsumer: CommsEventConsumer,
    private val clock: Clock,
) {

    @Transactional
    fun initiateMeeting(userId: UserId, command: InitiateMeetingCommand): MeetingIntentDTO {
        val meetingIntentId = meetingIntentRepository.nextIdentity()

        val request = command.toSlotSuggestionRequest()
        val suggestedSlots = slotSuggestionService.suggestSlots(userId, request).suggestions
            .map { MeetingSlot(it.startDateTime, it.endDateTime) }

        val meetingIntent = MeetingIntent(
            meetingIntentId,
            userId,
            command.durationMinutes.toMinutes(),
            command.timeZone,
            suggestedSlots
        ).let { meetingIntentRepository.save(it) }

        return meetingIntent.toDTO()
    }

    private fun InitiateMeetingCommand.toSlotSuggestionRequest(): SlotSuggestionRequest {
        return SlotSuggestionRequest(
            durationMinutes.toMinutes(),
            timeZone,
            suggestionSlotCount
        )
    }

    @Transactional
    fun proposeMeeting(userId: UserId, command: ProposeMeetingCommand): MeetingInvitationDTO {
        val meetingIntent = meetingIntentRepository.findByCodeOrThrow(command.meetingIntentCode)
        if (meetingIntent.initiatorId != userId) {
            throw AccessDeniedException("User#${userId.id} does not have access to MeetingIntent#${command.meetingIntentCode.code}")
        }

        val meetingId = meetingRepository.nextIdentity()
        val meetingCode = meetingCodeGenerator.generate()
        val meetingTitle = generateMeetingTitle(meetingIntent.initiatorId)

        val proposedSlots = command.proposedSlots.map { it.toValueObject() }
        val proposedMeeting = meetingIntent
            .propose(meetingId, meetingCode, proposedSlots, meetingTitle)
            .getOrThrow()
            .also { meetingRepository.save(it) }

        return meetingInvitationView.render(proposedMeeting, meetingIntent.timezone)
    }

    private fun generateMeetingTitle(initiatorId: UserId): String {
        val initiatorName = userService.find(initiatorId).fullName
        return initiatorName?.let { "Meeting with $it" } ?: "Meeting"
    }

    @Transactional(readOnly = true)
    fun loadMeetingProposal(meetingCode: MeetingCode): ProposedMeetingDTO {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)

        val (start, end) = proposedMeeting.proposedSlotsRange()
        val blockingCalendarEvents = eventApplicationService.fetchEvents(
            proposedMeeting.initiatorId,
            start.toLocalDate(), end.toLocalDate(), start.zone
        )
        val availableSlots = proposedMeeting.availableSlots(blockingCalendarEvents.events, clock)

        return meetingView.render(proposedMeeting, availableSlots)
    }

    @Transactional
    fun updateMeetingTitle(userId: UserId, meetingCode: MeetingCode, command: UpdateMeetingTitleCommand): Boolean {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        if (proposedMeeting.initiatorId != userId) {
            throw AccessDeniedException("User#${userId.id} does not have access to update Meeting#${meetingCode}")
        }

        meetingRepository.save(proposedMeeting.updateTitle(command.title))
        return true
    }

    @Transactional
    fun confirmMeeting(userId: UserId?, meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode, true)

        val meetingRecipient = if (command.recipientEmail != null) {
            try {
                userService.findInternalByEmail(command.recipientEmail)
                    .let { UserRecipient.ofUser(it) }
            } catch (e: NotFoundException) {
                EmailRecipient.of(command.recipientEmail)
            }
        } else {
            val user = userService.find(userId!!)
            UserRecipient.of(userId, user.email)
        }

        val confirmedMeeting = proposedMeeting
            .confirm(command.slot.toValueObject(), meetingRecipient)
            .getOrThrow()

        meetingRepository.save(confirmedMeeting)

        // "manual" event publishing
        val event = MeetingConfirmedEvent(confirmedMeeting)
        calendarEventConsumer.onMeetingConfirmed(event)
        commsEventConsumer.onMeetingConfirmed(event)
        return true
    }


    private fun findProposedMeetingOrThrow(meetingCode: MeetingCode, forUpdate: Boolean = false) =
        when (val meeting = meetingRepository.findByCodeOrThrow(meetingCode, forUpdate)) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meetingCode)
            is ExpiredMeeting -> throw NotFoundException(ProposedMeeting::class, meetingCode)
            else -> throw InvalidMeetingStatusException(meetingCode, meeting.status)
        }

    @SentryTransaction(operation = "expireMeetings")
    @Scheduled(cron = "0 0/15 * * * *")
    @Transactional
    fun expireMeetings() {
        val expiringMeetings = meetingRepository.findAllExpiring(ZonedDateTime.now(clock))
        if (expiringMeetings.isEmpty()) {
            return
        }
        meetingRepository.saveAllExpired(expiringMeetings)
    }
}

