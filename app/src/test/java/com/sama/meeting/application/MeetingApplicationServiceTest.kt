package com.sama.meeting.application

import com.sama.calendar.application.BlockEventConsumer
import com.sama.calendar.domain.BlockRepository
import com.sama.common.NotFoundException
import com.sama.meeting.domain.*
import com.sama.meeting.domain.aggregates.MeetingEntity
import com.sama.meeting.domain.aggregates.MeetingIntentEntity
import com.sama.meeting.domain.aggregates.MeetingProposedSlotEntity
import com.sama.meeting.domain.repositories.MeetingCodeGenerator
import com.sama.meeting.domain.repositories.MeetingIntentRepository
import com.sama.meeting.domain.repositories.MeetingRepository
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionResponse
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.slotsuggestion.domain.SlotSuggestion
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Clock
import java.time.Duration.ofMinutes
import java.time.Instant.ofEpochSecond
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime
import java.util.Optional.empty
import java.util.Optional.of
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class MeetingApplicationServiceTest(
    @Mock private val meetingIntentRepository: MeetingIntentRepository,
    @Mock private val meetingRepository: MeetingRepository,
    @Mock private val slotSuggestionService: SlotSuggestionService,
    @Mock private val meetingInvitationService: MeetingInvitationService,
    @Mock private val meetingCodeGenerator: MeetingCodeGenerator,
    @Mock private val userRepository: UserRepository,
    @Mock private val blockRepository: BlockRepository,
    @Mock private val blockEventConsumer: BlockEventConsumer,
) {
    private val clock: Clock = Clock.fixed(ofEpochSecond(3600), systemDefault())

    private lateinit var underTest: MeetingApplicationService

    @BeforeEach
    fun setup() {
        underTest = MeetingApplicationService(
            meetingIntentRepository,
            meetingRepository,
            slotSuggestionService,
            meetingInvitationService,
            meetingCodeGenerator,
            userRepository,
            blockRepository,
            blockEventConsumer,
            clock
        )
    }

    @Test
    fun `initiate meeting`() {
        // input
        val userId = 1L
        val meetingIntentId = 11L
        val command = InitiateMeetingCommand(30, systemDefault(), 1, 1)

        // setup
        whenever(meetingIntentRepository.nextIdentity())
            .thenReturn(meetingIntentId)

        val slotSuggestion = SlotSuggestion(
            ZonedDateTime.now(clock),
            ZonedDateTime.now(clock).plusMinutes(30),
            1.0
        )
        whenever(slotSuggestionService.suggestSlots(eq(userId), any()))
            .thenReturn(SlotSuggestionResponse(listOf(slotSuggestion)))

        // act
        val meetingIntent = underTest.initiateMeeting(userId, command)

        // verify
        val expectedSlotSuggestionRequest = SlotSuggestionRequest(
            ofMinutes(30),
            systemDefault(),
            1,
            LocalDateTime.now(clock),
            LocalDateTime.now(clock).plusDays(1)
        )
        verify(slotSuggestionService).suggestSlots(eq(userId), eq(expectedSlotSuggestionRequest))

        verify(meetingIntentRepository).save(any())

        val expectedDTO = MeetingIntentDTO(
            meetingIntentId, userId, RecipientDTO(null), 30,
            listOf(MeetingSlotDTO(ZonedDateTime.now(clock), ZonedDateTime.now(clock).plusMinutes(30)))
        )
        assertEquals(expectedDTO, meetingIntent)
    }

    @Test
    fun `initiate meeting without slot suggestions`() {
        // input
        val userId = 1L
        val meetingIntentId = 11L
        val command = InitiateMeetingCommand(30, systemDefault(), 0, 1)

        // setup
        whenever(meetingIntentRepository.nextIdentity())
            .thenReturn(meetingIntentId)

        // act
        val meetingIntent = underTest.initiateMeeting(userId, command)

        // verify
        verifyZeroInteractions(slotSuggestionService)
        verify(meetingIntentRepository).save(any())

        val expectedDTO = MeetingIntentDTO(
            meetingIntentId, userId, RecipientDTO(null), 30, emptyList()
        )
        assertEquals(expectedDTO, meetingIntent)
    }


    @Test
    fun `propose meeting`() {
        // input
        val initiatorId = 1L
        val initiatorFullName = "test"
        val initiatorEmail = "test@meetsama.com"
        val meetingId = 1L
        val meetingIntentId = 11L
        val meetingCode = "some-code"
        val meetingUrl = "https://meetsama.com/some-code"
        val shareableMessage = "message to share"
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.now(clock),
            ZonedDateTime.now(clock).plusMinutes(30)
        )
        val command = ProposeMeetingCommand(listOf(proposedSlot))

        // setup
        whenever(meetingIntentRepository.findById(eq(meetingIntentId)))
            .thenReturn(of(MeetingIntentEntity().apply {
                this.id = meetingIntentId
                this.initiatorId = initiatorId
                this.recipientId = null
                this.durationMinutes = 30
                this.timezone = systemDefault()
                this.suggestedSlots = mutableListOf()
            }))
        whenever(meetingRepository.nextIdentity()).thenReturn(meetingId)
        whenever(meetingCodeGenerator.generate()).thenReturn(meetingCode)
        whenever(meetingInvitationService.findForProposedMeeting(any(), any()))
            .thenReturn(MeetingInvitation(meetingUrl, shareableMessage))
        whenever(userRepository.findById(initiatorId))
            .thenReturn(of(UserEntity(initiatorEmail).apply { this.fullName = initiatorFullName }))

        // act
        val meetingInvitation = underTest.proposeMeeting(initiatorId, meetingIntentId, command)

        // verify
        verifyZeroInteractions(slotSuggestionService)
        verify(meetingRepository).save(any())

        val expectedDTO = MeetingInvitationDTO(
            ProposedMeetingDTO(listOf(proposedSlot), InitiatorDTO(initiatorFullName, initiatorEmail)),
            meetingUrl, shareableMessage
        )
        assertEquals(expectedDTO, meetingInvitation)
    }

    @Test
    fun `propose meeting without intent`() {
        // input
        val initiatorId = 1L
        val initiatorFullName = "test"
        val initiatorEmail = "test@meetsama.com"
        val meetingIntentId = 11L
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.now(clock),
            ZonedDateTime.now(clock).plusMinutes(30)
        )
        val command = ProposeMeetingCommand(listOf(proposedSlot))

        // setup
        whenever(meetingIntentRepository.findById(eq(meetingIntentId)))
            .thenReturn(empty())

        // act
        assertThrows<NotFoundException> { underTest.proposeMeeting(initiatorId, meetingIntentId, command) }
    }

    @Test
    fun `load meeting proposal from code`() {
        // input
        val initiatorId = 1L
        val initiatorFullName = "test"
        val initiatorEmail = "test@meetsama.com"
        val meetingId = 1L
        val meetingIntentId = 11L
        val meetingCode = "some-code"
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.now(clock).plusHours(3),
            ZonedDateTime.now(clock).plusHours(4)
        )

        // setup
        whenever(meetingRepository.findByCode(meetingCode))
            .thenReturn(MeetingEntity().apply {
                this.id = meetingId
                this.code = meetingCode
                this.meetingIntentId = meetingIntentId
                this.status = MeetingStatus.PROPOSED
                this.proposedSlots.add(
                    MeetingProposedSlotEntity(
                        1L,
                        meetingId,
                        proposedSlot.startDateTime,
                        proposedSlot.endDateTime
                    )
                )
            })
        whenever(meetingIntentRepository.findById(eq(meetingIntentId)))
            .thenReturn(of(MeetingIntentEntity().apply {
                this.id = meetingIntentId
                this.initiatorId = initiatorId
                this.recipientId = null
                this.durationMinutes = 60
                this.timezone = systemDefault()
                this.suggestedSlots = mutableListOf()
            }))

        whenever(userRepository.findById(initiatorId))
            .thenReturn(of(UserEntity(initiatorEmail).apply { this.fullName = initiatorFullName }))
        whenever(blockRepository.findAll(eq(initiatorId), any(), any()))
            .thenReturn(emptyList())

        // act
        val meetingInvitation = underTest.loadMeetingProposalFromCode(meetingCode)

        // verify
        val expectedDTO = ProposedMeetingDTO(listOf(proposedSlot), InitiatorDTO(initiatorFullName, initiatorEmail))
        assertEquals(expectedDTO, meetingInvitation)
    }

    @Test
    fun `load meeting proposal from non-existent code fails`() {
        val meetingCode = "some-code"

        // setup
        whenever(meetingRepository.findByCode(meetingCode))
            .thenThrow(NotFoundException::class.java)

        // act
        assertThrows<NotFoundException> { underTest.loadMeetingProposalFromCode(meetingCode) }
    }

    @TestFactory
    fun `load meeting proposal from invalid status fails`(): List<DynamicTest> {
        // input
        val userId = 1L
        val meetingId = 1L
        val meetingIntentId = 11L
        val meetingCode = "some-code"
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.now(clock).plusHours(3),
            ZonedDateTime.now(clock).plusHours(4)
        )
        whenever(meetingIntentRepository.findById(eq(meetingIntentId)))
            .thenReturn(of(MeetingIntentEntity().apply {
                this.id = meetingIntentId
                this.initiatorId = userId
                this.recipientId = null
                this.durationMinutes = 60
                this.timezone = systemDefault()
                this.suggestedSlots = mutableListOf()
            }))

        return listOf(
            MeetingStatus.CONFIRMED to MeetingAlreadyConfirmedException::class,
            MeetingStatus.EXPIRED to MeetingProposalExpiredException::class,
            MeetingStatus.REJECTED to InvalidMeetingStatusException::class
        ).map { (status, expected) ->
            DynamicTest.dynamicTest("Meeting status $status throws $expected") {
                whenever(meetingRepository.findByCode(meetingCode))
                    .thenReturn(MeetingEntity().apply {
                        this.status = status
                        this.id = meetingId
                        this.code = meetingCode
                        this.meetingIntentId = meetingIntentId
                        this.meetingRecipient = MeetingRecipient(null, "test@meetsama.com")
                        this.proposedSlots.add(
                            MeetingProposedSlotEntity(
                                1L,
                                meetingId,
                                proposedSlot.startDateTime,
                                proposedSlot.endDateTime
                            )
                        )
                        this.confirmedSlot = MeetingSlot(proposedSlot.startDateTime, proposedSlot.endDateTime)
                    })

                assertFailsWith(expected, "") { underTest.loadMeetingProposalFromCode(meetingCode) }
            }
        }
    }

    @Test
    fun `confirm meeting`() {
        val userId = 1L
        val meetingId = 1L
        val meetingIntentId = 11L
        val meetingCode = "some-code"
        val duration = 60L
        val slot = MeetingSlotDTO(
            ZonedDateTime.now(clock).plusHours(3),
            ZonedDateTime.now(clock).plusHours(4)
        )
        val recipientEmail = "test@meetsama.com"
        val command = ConfirmMeetingCommand(slot, recipientEmail)

        // setup
        whenever(meetingRepository.findByCode(meetingCode))
            .thenReturn(MeetingEntity().apply {
                this.id = meetingId
                this.code = meetingCode
                this.meetingIntentId = meetingIntentId
                this.status = MeetingStatus.PROPOSED
                this.proposedSlots.add(
                    MeetingProposedSlotEntity(
                        1L,
                        meetingId,
                        slot.startDateTime,
                        slot.endDateTime
                    )
                )
            })
        whenever(meetingIntentRepository.findById(eq(meetingIntentId)))
            .thenReturn(of(MeetingIntentEntity().apply {
                this.id = meetingIntentId
                this.initiatorId = userId
                this.recipientId = null
                this.durationMinutes = duration
                this.timezone = systemDefault()
                this.suggestedSlots = mutableListOf()
            }))


        // act
        val result = underTest.confirmMeeting(meetingCode, command)

        // verify
        verify(meetingRepository).save(any())

        val expectedEvent = MeetingConfirmedEvent(
            ConfirmedMeeting(
                meetingId,
                userId,
                ofMinutes(duration),
                MeetingRecipient(null, recipientEmail),
                slot.toValueObject()
            )
        )
        verify(blockEventConsumer).onMeetingConfirmed(eq(expectedEvent))

        assertEquals(true, result)
    }

    @Test
    fun `confirm non-existent meeting fails`() {
        val userId = 1L
        val meetingId = 1L
        val meetingIntentId = 11L
        val meetingCode = "some-code"
        val duration = 60L
        val slot = MeetingSlotDTO(
            ZonedDateTime.now(clock).plusHours(3),
            ZonedDateTime.now(clock).plusHours(4)
        )
        val recipientEmail = "test@meetsama.com"
        val command = ConfirmMeetingCommand(slot, recipientEmail)

        // setup
        whenever(meetingRepository.findByCode(meetingCode))
            .thenReturn(MeetingEntity().apply {
                this.id = meetingId
                this.code = meetingCode
                this.meetingIntentId = meetingIntentId
                this.status = MeetingStatus.PROPOSED
                this.proposedSlots.add(
                    MeetingProposedSlotEntity(
                        1L,
                        meetingId,
                        slot.startDateTime,
                        slot.endDateTime
                    )
                )
            })
        whenever(meetingIntentRepository.findById(eq(meetingIntentId)))
            .thenReturn(of(MeetingIntentEntity().apply {
                this.id = meetingIntentId
                this.initiatorId = userId
                this.recipientId = null
                this.durationMinutes = duration
                this.timezone = systemDefault()
                this.suggestedSlots = mutableListOf()
            }))

        // setup
        whenever(meetingRepository.findByCode(meetingCode))
            .thenThrow(NotFoundException::class.java)

        // act
        assertThrows<NotFoundException> { underTest.confirmMeeting(meetingCode, command) }
    }

    @TestFactory
    fun `confirm meeting from invalid status fails`(): List<DynamicTest> {
        // input
        val userId = 1L
        val meetingId = 1L
        val meetingIntentId = 11L
        val meetingCode = "some-code"
        val slot = MeetingSlotDTO(
            ZonedDateTime.now(clock).plusHours(3),
            ZonedDateTime.now(clock).plusHours(4)
        )
        val recipientEmail = "test@meetsama.com"
        val command = ConfirmMeetingCommand(slot, recipientEmail)

        whenever(meetingIntentRepository.findById(eq(meetingIntentId)))
            .thenReturn(of(MeetingIntentEntity().apply {
                this.id = meetingIntentId
                this.initiatorId = userId
                this.recipientId = null
                this.durationMinutes = 60
                this.timezone = systemDefault()
                this.suggestedSlots = mutableListOf()
            }))

        return listOf(
            MeetingStatus.CONFIRMED to MeetingAlreadyConfirmedException::class,
            MeetingStatus.EXPIRED to MeetingProposalExpiredException::class,
            MeetingStatus.REJECTED to InvalidMeetingStatusException::class
        ).map { (status, expected) ->
            DynamicTest.dynamicTest("Meeting status $status throws $expected") {
                whenever(meetingRepository.findByCode(meetingCode))
                    .thenReturn(MeetingEntity().apply {
                        this.id = meetingId
                        this.code = meetingCode
                        this.meetingIntentId = meetingIntentId
                        this.meetingRecipient = MeetingRecipient(null, null)
                        this.confirmedSlot = slot.toValueObject()
                        this.status = status
                        this.proposedSlots.add(
                            MeetingProposedSlotEntity(
                                1L,
                                meetingId,
                                slot.startDateTime,
                                slot.endDateTime
                            )
                        )
                    })

                assertFailsWith(expected, "") { underTest.confirmMeeting(meetingCode, command) }
            }
        }
    }

    @Test
    fun `expire meetings`() {
        val expiringMeetingIds = listOf(21L, 22L)
        whenever(meetingRepository.findAllIdsExpiring(any()))
            .thenReturn(expiringMeetingIds)

        underTest.expireMeetings()

        verify(meetingRepository).updateStatus(eq(MeetingStatus.EXPIRED), eq(expiringMeetingIds))
    }
}