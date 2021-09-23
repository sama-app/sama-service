package com.sama.meeting.application

import com.sama.calendar.application.CalendarEventConsumer
import com.sama.common.ApplicationService
import com.sama.common.DomainValidationException
import com.sama.common.NotFoundException
import com.sama.common.toMinutes
import com.sama.comms.application.CommsEventConsumer
import com.sama.connection.application.CreateUserConnectionCommand
import com.sama.connection.application.UserConnectionService
import com.sama.connection.domain.UserAlreadyConnectedException
import com.sama.meeting.domain.ConfirmedMeeting
import com.sama.meeting.domain.EmailRecipient
import com.sama.meeting.domain.ExpiredMeeting
import com.sama.meeting.domain.InvalidMeetingInitiationException
import com.sama.meeting.domain.InvalidMeetingStatusException
import com.sama.meeting.domain.Meeting
import com.sama.meeting.domain.MeetingAlreadyConfirmedException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingCodeGenerator
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.meeting.domain.MeetingIntent
import com.sama.meeting.domain.MeetingIntentRepository
import com.sama.meeting.domain.MeetingProposedEvent
import com.sama.meeting.domain.MeetingRepository
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.NewMeetingSlotsProposedEvent
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.SamaNonSamaProposedMeeting
import com.sama.meeting.domain.SamaSamaProposedMeeting
import com.sama.meeting.domain.UserRecipient
import com.sama.slotsuggestion.application.MultiUserSlotSuggestionRequest
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
    private val userService: InternalUserService,
    private val userConnectionService: UserConnectionService,
    private val calendarEventConsumer: CalendarEventConsumer,
    private val commsEventConsumer: CommsEventConsumer,
    private val clock: Clock,
) {

    fun dispatchInitiateMeetingCommand(userId: UserId, command: InitiateMeetingCommand): MeetingIntentDTO {
        return if (command.recipientId != null) {
            initiateSamaToSamaMeeting(userId, command.asSamaSama())
        } else {
            initiateSamaNonSamaMeeting(userId, command.asSamaNonSama())
        }
    }

    @Transactional
    fun initiateSamaNonSamaMeeting(initiatorId: UserId, command: InitiateSamaNonSamaMeetingCommand): MeetingIntentDTO {
        val meetingIntentId = meetingIntentRepository.nextIdentity()

        val request = SlotSuggestionRequest(
            command.durationMinutes.toMinutes(),
            command.timeZone,
            3
        )
        val suggestedSlots = slotSuggestionService.suggestSlots(initiatorId, request).suggestions
            .map { MeetingSlot(it.startDateTime, it.endDateTime) }

        val meetingIntent = MeetingIntent(
            meetingIntentId,
            initiatorId,
            command.durationMinutes.toMinutes(),
            null,
            command.timeZone,
            suggestedSlots
        ).let { meetingIntentRepository.save(it) }

        return meetingIntent.toDTO()
    }

    @Transactional
    fun initiateSamaToSamaMeeting(initiatorId: UserId, command: InitiateSamaSamaMeetingCommand): MeetingIntentDTO {
        val recipient = userService.findInternalByPublicId(command.recipientId)
        val usersConnected = userConnectionService.isConnected(initiatorId, recipient.id)
        if (!usersConnected) {
            throw InvalidMeetingInitiationException("User is not connected to User#${command.recipientId.id}")
        }

        val slotSuggestionRequest = MultiUserSlotSuggestionRequest(
            command.durationMinutes.toMinutes(),
            9,
            recipient.id
        )
        val suggestedSlots = slotSuggestionService.suggestSlots(initiatorId, slotSuggestionRequest).suggestions
            .map { MeetingSlot(it.startDateTime, it.endDateTime) }

        val meetingIntentId = meetingIntentRepository.nextIdentity()
        val meetingIntent = MeetingIntent(
            meetingIntentId,
            initiatorId,
            command.durationMinutes.toMinutes(),
            recipient.id,
            recipient.settings.timeZone,
            suggestedSlots
        ).let { meetingIntentRepository.save(it) }

        return meetingIntent.toDTO()
    }

    @Transactional
    fun proposeMeeting(userId: UserId, command: ProposeMeetingCommand): MeetingInvitationDTO {
        val meetingIntent = meetingIntentRepository.findByCodeOrThrow(command.meetingIntentCode)
        if (!meetingIntent.isReadableBy(userId)) {
            throw AccessDeniedException("User does not have access to MeetingIntent#${command.meetingIntentCode.code}")
        }

        val meetingId = meetingRepository.nextIdentity()
        val meetingCode = meetingCodeGenerator.generate()
        val meetingTitle = generateMeetingTitle(meetingIntent)

        val proposedSlots = command.proposedSlots.map { it.toValueObject() }
        val proposedMeeting = meetingIntent
            .propose(meetingId, meetingCode, proposedSlots, meetingTitle)
            .also { meetingRepository.save(it) }

        val event = MeetingProposedEvent(userId, proposedMeeting)
        commsEventConsumer.onMeetingProposed(event)

        return meetingInvitationView.render(proposedMeeting, meetingIntent.recipientTimeZone)
    }

    private fun generateMeetingTitle(meetingIntent: MeetingIntent): String {
        val initiatorName = userService.find(meetingIntent.initiatorId)
            .let { it.fullName ?: it.email }
        val recipientName = meetingIntent.recipientId
            ?.let { userService.find(it) }
            ?.let { it.fullName ?: it.email }
        return when {
            meetingIntent.isSamaSama -> "$initiatorName / $recipientName"
            else -> "Meeting with $initiatorName"
        }
    }

    @Transactional
    fun loadMeetingProposal(userId: UserId?, meetingCode: MeetingCode): ProposedMeetingDTO {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        if (!proposedMeeting.isReadableBy(userId)) {
            throw AccessDeniedException("User does not have access to read Meeting#${meetingCode}")
        }

        return when (proposedMeeting) {
            is SamaNonSamaProposedMeeting -> meetingView.render(userId, proposedMeeting)
            is SamaSamaProposedMeeting -> meetingView.render(userId, proposedMeeting)
        }
    }

    @Transactional
    fun proposeNewMeetingSlots(userId: UserId, meetingCode: MeetingCode, command: ProposeNewMeetingSlotsCommand): Boolean {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode, true)
        if (!proposedMeeting.isModifiableBy(userId)) {
            throw AccessDeniedException("User does not have access to modify Meeting#${meetingCode}")
        }
        check(proposedMeeting is SamaSamaProposedMeeting)

        val updated = proposedMeeting
            .proposeNewSlots(command.proposedSlots.map { it.toValueObject() })

        meetingRepository.save(updated)

        val event = NewMeetingSlotsProposedEvent(userId, proposedMeeting)
        commsEventConsumer.onNewMeetingSlotsProposed(event)
        return true
    }

    @Transactional
    fun updateMeetingTitle(userId: UserId, meetingCode: MeetingCode, command: UpdateMeetingTitleCommand): Boolean {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        when (proposedMeeting) {
            is SamaNonSamaProposedMeeting -> if (proposedMeeting.initiatorId != userId) {
                throw AccessDeniedException("User does not have access to modify Meeting#${meetingCode}")
            }
            is SamaSamaProposedMeeting -> if (!proposedMeeting.isModifiableBy(userId)) {
                throw AccessDeniedException("User does not have access to modify Meeting#${meetingCode}")
            }
        }

        meetingRepository.save(proposedMeeting.updateTitle(command.title))
        return true
    }

    @Transactional
    fun connectWithInitiator(userId: UserId, meetingCode: MeetingCode, command: ConnectWithMeetingInitiatorCommand): Boolean {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        if (!proposedMeeting.isModifiableBy(userId)) {
            throw AccessDeniedException("User does not have access to modify Meeting#${meetingCode}")
        }
        check(proposedMeeting is SamaNonSamaProposedMeeting)

        try {
            userConnectionService.createUserConnection(userId, CreateUserConnectionCommand(proposedMeeting.initiatorId))
        } catch (ignored: UserAlreadyConnectedException) {
        }

        val updated = proposedMeeting.claimAsRecipient(userId)
        meetingRepository.save(updated)
        return true
    }

    @Transactional
    fun confirmMeeting(userId: UserId?, meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean {
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode, true)
        if (!proposedMeeting.isModifiableBy(userId)) {
            throw AccessDeniedException("User does not have access to modify Meeting#${meetingCode}")
        }

        val confirmedSlot = command.slot.toValueObject()

        val confirmedMeeting = when (proposedMeeting) {
            is SamaSamaProposedMeeting -> {
                proposedMeeting.confirm(confirmedSlot)
            }

            is SamaNonSamaProposedMeeting -> {
                val recipient = meetingRecipient(command.recipientEmail, userId)
                    ?: throw DomainValidationException("Missing recipient details")
                proposedMeeting.confirm(confirmedSlot, recipient)
            }
        }

        meetingRepository.save(confirmedMeeting)

        // "manual" event publishing
        val event = MeetingConfirmedEvent(userId, confirmedMeeting)
        calendarEventConsumer.onMeetingConfirmed(event)
        commsEventConsumer.onMeetingConfirmed(event)
        return true
    }

    private fun meetingRecipient(recipientEmail: String?, userId: UserId?) = when {
        recipientEmail != null -> {
            try {
                val user = userService.findInternalByEmail(recipientEmail)
                UserRecipient.of(user.id)
            } catch (e: NotFoundException) {
                EmailRecipient.of(recipientEmail)
            }
        }
        userId != null -> UserRecipient.of(userId)
        else -> null
    }

    private fun findProposedMeetingOrThrow(meetingCode: MeetingCode, forUpdate: Boolean = false) =
        when (val meeting = meetingRepository.findByCodeOrThrow(meetingCode, forUpdate)) {
            is ProposedMeeting -> meeting
            is ConfirmedMeeting -> throw MeetingAlreadyConfirmedException(meetingCode)
            is ExpiredMeeting -> throw NotFoundException(Meeting::class, meetingCode)
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

