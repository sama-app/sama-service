package com.sama.meeting.application

import com.sama.calendar.application.CalendarEventConsumer
import com.sama.calendar.application.EventSearchCriteria
import com.sama.calendar.application.EventService
import com.sama.common.ApplicationService
import com.sama.common.DomainValidationException
import com.sama.common.NotFoundException
import com.sama.common.checkAccess
import com.sama.common.execute
import com.sama.common.toMinutes
import com.sama.comms.application.CommsEventConsumer
import com.sama.connection.application.CreateUserConnectionCommand
import com.sama.connection.application.UserConnectionService
import com.sama.connection.domain.UserAlreadyConnectedException
import com.sama.meeting.domain.AvailableSlots
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
import com.sama.meeting.domain.MeetingPreferences
import com.sama.meeting.domain.MeetingProposedEvent
import com.sama.meeting.domain.MeetingRepository
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.MeetingSlotUnavailableException
import com.sama.meeting.domain.NewMeetingSlotsProposedEvent
import com.sama.meeting.domain.ProposedMeeting
import com.sama.meeting.domain.SamaNonSamaProposedMeeting
import com.sama.meeting.domain.SamaSamaProposedMeeting
import com.sama.meeting.domain.UserRecipient
import com.sama.slotsuggestion.application.HeatMapService
import com.sama.slotsuggestion.application.MultiUserSlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.users.application.AuthUserService
import com.sama.users.application.InternalUserService
import com.sama.users.domain.UserId
import io.sentry.spring.tracing.SentryTransaction
import java.time.Clock
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.math.ceil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class MeetingApplicationService(
    private val meetingIntentRepository: MeetingIntentRepository,
    private val meetingRepository: MeetingRepository,
    private val slotSuggestionService: SlotSuggestionService,
    private val heatMapService: HeatMapService,
    private val meetingInvitationView: MeetingInvitationView,
    private val meetingView: MeetingView,
    private val meetingCodeGenerator: MeetingCodeGenerator,
    private val userService: InternalUserService,
    private val userConnectionService: UserConnectionService,
    private val eventService: EventService,
    private val authUserService: AuthUserService,
    private val calendarEventConsumer: CalendarEventConsumer,
    private val commsEventConsumer: CommsEventConsumer,
    private val taskScheduler: TaskScheduler,
    private val clock: Clock,
) : MeetingService {
    private var logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun dispatchInitiateMeetingCommand(command: InitiateMeetingCommand): MeetingIntentDTO {
        val userId = authUserService.currentUserId()
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
            3,
            command.timeZone
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
        val meetingTitle = generateMeetingTitle(meetingIntent)

        return meetingIntent.toDTO(meetingTitle)
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
        val meetingTitle = generateMeetingTitle(meetingIntent)

        return meetingIntent.toDTO(meetingTitle)
    }

    @Transactional
    override fun proposeMeeting(command: ProposeMeetingCommand): MeetingInvitationDTO {
        val userId = authUserService.currentUserId()
        val meetingIntent = meetingIntentRepository.findByCodeOrThrow(command.meetingIntentCode)
        checkAccess(meetingIntent.isReadableBy(userId)) { "User does not have access to read MeetingIntent#${meetingIntent.code}" }

        val meetingId = meetingRepository.nextIdentity()
        val meetingCode = meetingCodeGenerator.generate()
        val meetingTitle = command.title ?: generateMeetingTitle(meetingIntent)

        val proposedSlots = command.proposedSlots.map { it.toValueObject() }
        val proposedMeeting = meetingIntent
            .propose(
                meetingId,
                meetingCode,
                proposedSlots,
                meetingTitle,
                MeetingPreferences(blockOutSlots = command.blockOutSlots ?: false)
            )
            .also { meetingRepository.save(it) }

        val event = MeetingProposedEvent(userId, proposedMeeting)
        taskScheduler.execute { calendarEventConsumer.onMeetingProposed(event) }
        taskScheduler.execute { commsEventConsumer.onMeetingProposed(event) }

        return meetingInvitationView.render(proposedMeeting, meetingIntent.recipientTimeZone)
    }

    private fun generateMeetingTitle(meetingIntent: MeetingIntent): String {
        val initiatorUser = userService.findInternal(meetingIntent.initiatorId)
        initiatorUser.settings.meetingPreferences.defaultTitle
            ?.let { return it }

        val initiatorName = initiatorUser
            .let { it.fullName ?: it.email }
        val recipientName = meetingIntent.recipientId
            ?.let { userService.find(it) }
            ?.let { it.fullName ?: it.email }
        return when {
            meetingIntent.isSamaSama -> "$initiatorName / $recipientName"
            else -> "Meeting with $initiatorName"
        }
    }

    // TODO: Temporary hack
    @Transactional
    override fun createFullAvailabilityLink(command: CreateFullAvailabilityLinkCommand): String {
        val user = authUserService.currentUser()
        val duration = command.durationMinutes.toMinutes()
        val meetingIntent = MeetingIntent(
            meetingIntentRepository.nextIdentity(),
            user.id,
            duration,
            null,
            user.settings.timeZone,
            emptyList()
        ).let { meetingIntentRepository.save(it) }

        val meetingId = meetingRepository.nextIdentity()
        val meetingCode = MeetingCode(command.meetingCode)
        val meetingTitle = generateMeetingTitle(meetingIntent)

        val heatMap = heatMapService.generateFreeBusy(user.id)
        val slotWindowSize = ceil(duration.toMinutes().toDouble() / heatMap.intervalMinutes).toInt()

        val freeSlots = heatMap.slots.asSequence()
            .map { it.totalWeight }
            .windowed(slotWindowSize) { it.reduce { acc, d -> acc + d } }
            .withIndex()
            .filter { (_, value) -> value == 0.0 }
            .map { (idx, _) ->
                val start = heatMap.slots[idx].startDateTime.atZone(heatMap.userTimeZone)
                val end = start.plus(duration)
                MeetingSlot(start, end)
            }
            .toList()

        var proposedMeeting = meetingIntent
            .propose(meetingId, meetingCode, freeSlots, meetingTitle, MeetingPreferences.default())
        check(proposedMeeting is SamaNonSamaProposedMeeting)

        proposedMeeting = proposedMeeting.makeLinkPermanent()
        meetingRepository.save(proposedMeeting)

        return meetingInvitationView.renderMeetingUrl(meetingCode)
    }

    @Transactional
    override fun loadMeetingProposal(meetingCode: MeetingCode): ProposedMeetingDTO {
        val currentUserId = authUserService.currentUserIdOrNull()
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        checkAccess(proposedMeeting.isReadableBy(currentUserId)) { "User does not have access to read Meeting#${meetingCode}" }

        return when (proposedMeeting) {
            is SamaNonSamaProposedMeeting -> {
                val availableSlots = availableSlots(proposedMeeting)
                meetingView.render(currentUserId, proposedMeeting, availableSlots)
            }

            is SamaSamaProposedMeeting -> meetingView.render(currentUserId, proposedMeeting)
        }
    }

    @Transactional
    override fun proposeNewMeetingSlots(meetingCode: MeetingCode, command: ProposeNewMeetingSlotsCommand): Boolean {
        val userId = authUserService.currentUserId()
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode, true)
        checkAccess(proposedMeeting.isModifiableBy(userId)) { "User does not have access to modify Meeting#${meetingCode}" }
        require(proposedMeeting is SamaSamaProposedMeeting)

        val updated = proposedMeeting
            .proposeNewSlots(command.proposedSlots.map { it.toValueObject() })

        meetingRepository.save(updated)

        val event = NewMeetingSlotsProposedEvent(userId, proposedMeeting)
        commsEventConsumer.onNewMeetingSlotsProposed(event)
        return true
    }

    @Transactional(readOnly = true)
    override fun getSlotSuggestions(meetingCode: MeetingCode): MeetingSlotSuggestionDTO {
        val userId = authUserService.currentUserId()
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        checkAccess(proposedMeeting.isReadableBy(userId))
        { "User does not have access to Meeting#${meetingCode.code}" }

        check(proposedMeeting is SamaSamaProposedMeeting)

        val recipientId = checkNotNull(proposedMeeting.otherActorId(userId))
        val request = MultiUserSlotSuggestionRequest(proposedMeeting.duration, 9, recipientId)
        val slotSuggestions = slotSuggestionService.suggestSlots(userId, request)

        val rejectedSlots = proposedMeeting.rejectedSlots
        val suggestedSlots = slotSuggestions.suggestions
            .map { MeetingSlot(it.startDateTime, it.endDateTime) }
            .minus(rejectedSlots)

        return MeetingSlotSuggestionDTO(suggestedSlots.toDTO(), rejectedSlots.toDTO())
    }

    @Transactional
    override fun updateMeetingTitle(meetingCode: MeetingCode, command: UpdateMeetingTitleCommand): Boolean {
        val userId = authUserService.currentUserId()
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        when (proposedMeeting) {
            is SamaNonSamaProposedMeeting -> {
                checkAccess(proposedMeeting.initiatorId == userId)
                { "User does not have access to modify Meeting#${meetingCode}" }
            }
            is SamaSamaProposedMeeting -> {
                checkAccess(proposedMeeting.isModifiableBy(userId))
                { "User does not have access to modify Meeting#${meetingCode}" }
            }
        }

        meetingRepository.save(proposedMeeting.updateTitle(command.title))
        return true
    }

    @Transactional
    override fun connectWithInitiator(meetingCode: MeetingCode, command: ConnectWithMeetingInitiatorCommand): Boolean {
        val userId = authUserService.currentUserId()
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode)
        checkAccess(proposedMeeting.isModifiableBy(userId)) { "User does not have access to modify Meeting#${meetingCode}" }
        require(proposedMeeting is SamaNonSamaProposedMeeting)

        try {
            userConnectionService.createUserConnection(userId, CreateUserConnectionCommand(proposedMeeting.initiatorId))
        } catch (ignored: UserAlreadyConnectedException) {
        }

        val updated = proposedMeeting.claimAsRecipient(userId)
        meetingRepository.save(updated)
        return true
    }

    @Transactional
    override fun confirmMeeting(meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean {
        val currentUserId = authUserService.currentUserIdOrNull()
        val proposedMeeting = findProposedMeetingOrThrow(meetingCode, true)
        checkAccess(proposedMeeting.isModifiableBy(currentUserId)) { "User does not have access to modify Meeting#${meetingCode}" }

        val confirmedSlot = command.slot.toValueObject()

        val confirmedMeeting = when (proposedMeeting) {
            is SamaSamaProposedMeeting -> {
                proposedMeeting.confirm(confirmedSlot)
            }

            is SamaNonSamaProposedMeeting -> {
                val recipient = meetingRecipient(command.recipientEmail, currentUserId)
                    ?: throw DomainValidationException("Missing recipient details")
                val availableSlots = availableSlots(proposedMeeting)
                if (!availableSlots.isSlotAvailable(confirmedSlot)) {
                    throw MeetingSlotUnavailableException(meetingCode, confirmedSlot)
                }
                proposedMeeting.confirm(confirmedSlot, recipient)
            }
        }

        // TODO: temporary hack for permalinks
        if (!proposedMeeting.meetingPreferences.permanentLink) {
            meetingRepository.save(confirmedMeeting)
        }

        // "manual" event publishing
        val event = MeetingConfirmedEvent(currentUserId, confirmedMeeting)
        taskScheduler.execute { calendarEventConsumer.onMeetingConfirmed(event) }
        taskScheduler.execute { commsEventConsumer.onMeetingConfirmed(event) }
        return true
    }

    private fun availableSlots(proposedMeeting: SamaNonSamaProposedMeeting): AvailableSlots {
        val (start, end) = proposedMeeting.proposedSlotsRange()
        val meetingProposedAt = proposedMeeting.createdAt!!
        val searchCriteria = EventSearchCriteria(createdFrom = meetingProposedAt, hasAttendees = true)
        val blockingCalendarEvents = eventService.fetchEvents(
            proposedMeeting.initiatorId, start.toLocalDate(), end.toLocalDate(), UTC, searchCriteria
        ).events
        return AvailableSlots.of(proposedMeeting, blockingCalendarEvents, clock)
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
        logger.info("Expired ${expiringMeetings.size} meetings... ")
    }
}

