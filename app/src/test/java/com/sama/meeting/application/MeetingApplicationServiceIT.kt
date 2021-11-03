package com.sama.meeting.application

import com.ninjasquad.springmockk.MockkBean
import com.sama.calendar.application.CalendarEventConsumer
import com.sama.calendar.application.EventDTO
import com.sama.calendar.application.EventService
import com.sama.calendar.application.EventsDTO
import com.sama.common.BaseApplicationIntegrationTest
import com.sama.common.NotFoundException
import com.sama.comms.application.CommsEventConsumer
import com.sama.connection.application.CreateUserConnectionCommand
import com.sama.connection.application.UserConnectionService
import com.sama.integration.google.auth.domain.GoogleAccountPublicId
import com.sama.meeting.domain.InvalidMeetingInitiationException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingSlotUnavailableException
import com.sama.slotsuggestion.application.MultiUserSlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionResponse
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.slotsuggestion.domain.SlotSuggestion
import io.mockk.every
import io.mockk.verify
import java.time.Clock
import java.time.Duration.ofMinutes
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class MeetingApplicationServiceIT : BaseApplicationIntegrationTest() {

    @MockkBean
    lateinit var slotSuggestionService: SlotSuggestionService

    @MockkBean
    lateinit var userConnectionService: UserConnectionService

    @MockkBean
    lateinit var eventService: EventService

    @MockkBean(relaxed = true)
    lateinit var calendarEventConsumer: CalendarEventConsumer

    @MockkBean(relaxed = true)
    lateinit var commsEventConsumer: CommsEventConsumer

    @Autowired
    lateinit var clock: Clock

    @Autowired
    lateinit var underTest: MeetingApplicationService

    @Test
    fun `setup sama-non-sama meeting`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id, SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0))))

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(
                    60, clock.zone
                )
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot),
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        // load proposal from meeting code with initiator's calendar non-blocked
        every {
            eventService.fetchEvents(
                initiator().id, proposedSlotStart.toLocalDate(), proposedSlotEnd.toLocalDate(), UTC, any()
            )
        } returns EventsDTO(emptyList())

        val meetingProposal = underTest.loadMeetingProposal(meetingInvitationDTO.meetingCode)
        assertThat(meetingProposal.title).isEqualTo("Meeting with ${initiator().fullName}") // Verify default title created

        // confirm meeting
        underTest.confirmMeeting(
            meetingInvitationDTO.meetingCode,
            ConfirmMeetingCommand(
                meetingProposal.proposedSlots[0],
                "non-sama-recipient@meetsama.com"
            )
        )

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }


    @Test
    fun `setup sama-sama meeting`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            every {
                slotSuggestionService
                    .suggestSlots(it.id, MultiUserSlotSuggestionRequest(ofMinutes(60), 9, recipient().id))
            }.returns(SlotSuggestionResponse(emptyList()))

            every { userConnectionService.isConnected(initiator().id, recipient().id) } returns true

            underTest.initiateSamaToSamaMeeting(
                it.id, InitiateSamaSamaMeetingCommand(
                    60, recipient().publicId
                )
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(1)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)

        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        asRecipient {
            underTest.confirmMeeting(
                meetingInvitationDTO.meetingCode,
                ConfirmMeetingCommand(proposedSlot, null)
            )
        }

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }

    @Test
    fun `setup sama-sama let recipient pick meeting`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            every {
                slotSuggestionService
                    .suggestSlots(it.id, MultiUserSlotSuggestionRequest(ofMinutes(60), 9, recipient().id))
            }.returns(SlotSuggestionResponse(emptyList()))

            every { userConnectionService.isConnected(initiator().id, recipient().id) } returns true

            underTest.initiateSamaToSamaMeeting(
                it.id, InitiateSamaSamaMeetingCommand(
                    60, recipient().publicId
                )
            )
        }

        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    emptyList()
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        // propose new time
        val now = ZonedDateTime.now(clock)
        val meetingCode = meetingInvitationDTO.meetingCode
        val proposedSlot = MeetingSlotDTO(now.plusHours(3), now.plusHours(4))
        asRecipient {
            underTest.proposeNewMeetingSlots(
                meetingCode,
                ProposeNewMeetingSlotsCommand(listOf(proposedSlot))
            )
        }


        asInitiator {
            underTest.confirmMeeting(meetingCode, ConfirmMeetingCommand(proposedSlot, null))
        }

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }

    @Test
    fun `setup sama-sama back and forth meeting`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            every {
                slotSuggestionService
                    .suggestSlots(it.id, MultiUserSlotSuggestionRequest(ofMinutes(60), 9, recipient().id))
            }.returns(SlotSuggestionResponse(emptyList()))

            every { userConnectionService.isConnected(initiator().id, recipient().id) } returns true

            underTest.initiateSamaToSamaMeeting(
                it.id, InitiateSamaSamaMeetingCommand(
                    60, recipient().publicId
                )
            )
        }

        // propose meeting
        val now = ZonedDateTime.now(clock)
        val proposedSlot = MeetingSlotDTO(now.plusHours(2), now.plusHours(3))

        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        // propose new time
        val meetingCode = meetingInvitationDTO.meetingCode
        val newProposedSlot = MeetingSlotDTO(now.plusHours(3), now.plusHours(4))
        asRecipient {
            underTest.proposeNewMeetingSlots(
                meetingCode,
                ProposeNewMeetingSlotsCommand(listOf(newProposedSlot))
            )
        }

        val newProposedSlot2 = MeetingSlotDTO(now.plusHours(4), now.plusHours(5))
        asInitiator {
            underTest.proposeNewMeetingSlots(
                meetingCode,
                ProposeNewMeetingSlotsCommand(listOf(newProposedSlot2))
            )
        }

        val newProposedSlot3 = MeetingSlotDTO(now.plusHours(5), now.plusHours(6))
        asRecipient {
            underTest.proposeNewMeetingSlots(
                meetingCode,
                ProposeNewMeetingSlotsCommand(listOf(newProposedSlot3))
            )
        }

        asInitiator {
            underTest.confirmMeeting(meetingCode, ConfirmMeetingCommand(newProposedSlot3, null))
        }

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }

    @Test
    fun `only connected users can setup sama-sama meeting`() {
        every { userConnectionService.isConnected(initiator().id, recipient().id) } returns false

        assertThrows<InvalidMeetingInitiationException> {
            asInitiator {
                underTest.initiateSamaToSamaMeeting(
                    it.id, InitiateSamaSamaMeetingCommand(
                        60, recipient().publicId
                    )
                )
            }
        }
    }

    @Test
    fun `cannot modify sama-sama meeting when not current actor`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            every {
                slotSuggestionService
                    .suggestSlots(it.id, MultiUserSlotSuggestionRequest(ofMinutes(60), 9, recipient().id))
            }.returns(SlotSuggestionResponse(emptyList()))

            every { userConnectionService.isConnected(initiator().id, recipient().id) } returns true

            underTest.initiateSamaToSamaMeeting(
                it.id, InitiateSamaSamaMeetingCommand(
                    60, recipient().publicId
                )
            )
        }

        // propose meeting
        val now = ZonedDateTime.now(clock)
        val proposedSlot = MeetingSlotDTO(now.plusHours(2), now.plusHours(3))

        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        // propose new time
        val meetingCode = meetingInvitationDTO.meetingCode
        val newProposedSlot = MeetingSlotDTO(now.plusHours(3), now.plusHours(4))

        assertThrows<AccessDeniedException> {
            asInitiator {
                underTest.proposeNewMeetingSlots(
                    meetingCode,
                    ProposeNewMeetingSlotsCommand(listOf(newProposedSlot))
                )
            }
        }

        assertThrows<AccessDeniedException> {
            asInitiator {
                underTest.confirmMeeting(
                    meetingCode,
                    ConfirmMeetingCommand(newProposedSlot, null)
                )
            }
        }

        // switch actor
        asRecipient {
            underTest.proposeNewMeetingSlots(
                meetingCode,
                ProposeNewMeetingSlotsCommand(listOf(newProposedSlot))
            )
        }

        assertThrows<AccessDeniedException> {
            asRecipient {
                underTest.proposeNewMeetingSlots(
                    meetingCode,
                    ProposeNewMeetingSlotsCommand(listOf(newProposedSlot))
                )
            }
        }

        assertThrows<AccessDeniedException> {
            asRecipient {
                underTest.confirmMeeting(
                    meetingCode,
                    ConfirmMeetingCommand(newProposedSlot, null)
                )
            }
        }

    }

    @Test
    fun `suggested slots for sama-non-sama meeting not allowed`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id,
                    SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(
                    60, clock.zone,
                )
            )
        }

        // propose meeting
        val now = ZonedDateTime.now(clock)
        val proposedSlot = MeetingSlotDTO(now.plusHours(2), now.plusHours(3))

        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        assertThrows<IllegalStateException> {
            asInitiator { underTest.getSlotSuggestions(meetingInvitationDTO.meetingCode) }
        }
    }

    @Test
    fun `suggested slots for sama-sama meeting`() {

        val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
        val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
        every {
            slotSuggestionService.suggestSlots(
                initiator().id,
                MultiUserSlotSuggestionRequest(ofMinutes(60), 9, recipient().id)
            )
        }.returns(SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0))))

        every {
            slotSuggestionService.suggestSlots(
                recipient().id,
                MultiUserSlotSuggestionRequest(ofMinutes(60), 9, initiator().id)
            )
        }.returns(SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0))))

        // create meeting intent
        val meetingIntentDTO = asInitiator {
            every { (userConnectionService.isConnected(initiator().id, recipient().id)) } returns true

            underTest.initiateSamaToSamaMeeting(
                it.id, InitiateSamaSamaMeetingCommand(60, recipient().publicId)
            )
        }

        // propose meeting
        val suggestedSlot = meetingIntentDTO.suggestedSlots[0]
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(suggestedSlot)
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        asRecipient {
            val r = underTest.getSlotSuggestions(meetingInvitationDTO.meetingCode)
            assertThat(r.suggestedSlots).containsExactly(suggestedSlot)
            assertThat(r.rejectedSlots).isEmpty()
        }

        asInitiator {
            val r = underTest.getSlotSuggestions(meetingInvitationDTO.meetingCode)
            assertThat(r.suggestedSlots).containsExactly(suggestedSlot)
            assertThat(r.rejectedSlots).isEmpty()
        }

        // propose new slot should add it as rejected
        val now = ZonedDateTime.now(clock)
        val proposedSlot = MeetingSlotDTO(now.plusHours(2), now.plusHours(3))
        asRecipient {
            underTest.proposeNewMeetingSlots(
                meetingInvitationDTO.meetingCode, ProposeNewMeetingSlotsCommand(listOf(proposedSlot))
            )
        }

        asRecipient {
            val r = underTest.getSlotSuggestions(meetingInvitationDTO.meetingCode)
            assertThat(r.suggestedSlots).isEmpty()
            assertThat(r.rejectedSlots).containsExactly(suggestedSlot)
        }

        asInitiator {
            val r = underTest.getSlotSuggestions(meetingInvitationDTO.meetingCode)
            assertThat(r.suggestedSlots).isEmpty()
            assertThat(r.rejectedSlots).containsExactly(suggestedSlot)
        }
    }

    @Test
    fun `only initiator can propose meetings from their own intent`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id,
                    SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(
                    60, clock.zone,
                )
            )
        }

        assertThrows<AccessDeniedException> {
            asRecipient {
                underTest.proposeMeeting(
                    ProposeMeetingCommand(
                        meetingIntentDTO.meetingIntentCode,
                        listOf()
                    )
                )
            }
        }
    }


    @Test
    fun `setup sama-non-sama meeting with custom title`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id, SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(60, clock.zone)
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val initialTitle = "My initial title"
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot),
                    initialTitle
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        every {
            eventService.fetchEvents(
                initiator().id, proposedSlotStart.toLocalDate(), proposedSlotEnd.toLocalDate(), UTC, any()
            )
        } returns EventsDTO(emptyList())

        run {
            val meetingProposal = underTest.loadMeetingProposal(meetingInvitationDTO.meetingCode)
            assertThat(meetingProposal.title).isEqualTo(initialTitle) // verify new title is here
        }

        // update title
        val meetingTitle = "My new fancy title"
        asInitiator {
            underTest.updateMeetingTitle(
                meetingInvitationDTO.meetingCode, UpdateMeetingTitleCommand(meetingTitle)
            )
        }

        val meetingProposal = underTest.loadMeetingProposal(meetingInvitationDTO.meetingCode)
        assertThat(meetingProposal.title).isEqualTo(meetingTitle) // verify new title is here

        // confirm meeting
        underTest.confirmMeeting(
            meetingInvitationDTO.meetingCode,
            ConfirmMeetingCommand(
                meetingProposal.proposedSlots[0],
                "non-sama-recipient@meetsama.com"
            )
        )

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }

    @Test
    fun `setup sama-non-sama meeting when all initiators proposed slots are blocked`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id,
                    SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0))))

            underTest.initiateSamaNonSamaMeeting(
                it.id,
                InitiateSamaNonSamaMeetingCommand(60, clock.zone)
            )
        }

        // propose meeting with two slots
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(1)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)

        val proposedSlotTwoStart = ZonedDateTime.now(clock).plusDays(1).plusHours(2)
        val proposedSlotTwoEnd = proposedSlotTwoStart.plusDays(1).plusHours(1)
        val proposedSlotTwo = MeetingSlotDTO(proposedSlotTwoStart, proposedSlotTwoEnd)

        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot, proposedSlotTwo)
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        // load proposal from meeting code with initiator's calendar blocked completely
        every {
            eventService.fetchEvents(
                initiator().id,
                proposedSlotStart.toLocalDate(),
                proposedSlotTwoEnd.toLocalDate(),
                UTC,
                any()
            )
        }.returns(
            EventsDTO(
                listOf(
                    EventDTO(
                        proposedSlotStart, proposedSlotTwoEnd, false, "Title",
                        GoogleAccountPublicId(UUID.randomUUID()), "primary", "eventId", false
                    )
                )
            )
        )
        val meetingProposal = underTest.loadMeetingProposal(meetingInvitationDTO.meetingCode)

        // no proposed slots available
        assertThat(meetingProposal.proposedSlots).isEmpty()

        // try to confirm meeting
        assertThrows<MeetingSlotUnavailableException> {
            underTest.confirmMeeting(
                meetingInvitationDTO.meetingCode,
                ConfirmMeetingCommand(
                    proposedSlot,
                    recipient().email
                )
            )
        }

        verify(exactly = 0) { calendarEventConsumer.onMeetingConfirmed(any()) }
    }

    @Test
    fun `setup sama-non-sama meeting with existing sama user`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id,
                    SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(
                    60, clock.zone,
                )
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        // load proposal from meeting code with initiator's calendar non-blocked
        every {
            eventService.fetchEvents(
                initiator().id, proposedSlotStart.toLocalDate(), proposedSlotEnd.toLocalDate(), UTC, any()
            )
        } returns EventsDTO(emptyList())

        // confirm meeting
        asRecipient {
            val meetingProposal = underTest.loadMeetingProposal(meetingInvitationDTO.meetingCode)
            assertThat(meetingProposal.isOwnMeeting).isFalse()

            underTest.confirmMeeting(
                meetingInvitationDTO.meetingCode,
                ConfirmMeetingCommand(
                    meetingProposal.proposedSlots[0],
                    null
                )
            )
        }

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }


    @Test
    fun `setup sama-non-sama meeting with yourself`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id,
                    SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(
                    60, clock.zone,
                )
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        verify { calendarEventConsumer.onMeetingProposed(any()) }

        // load proposal from meeting code with initiator's calendar non-blocked
        every {
            eventService.fetchEvents(
                initiator().id, proposedSlotStart.toLocalDate(), proposedSlotEnd.toLocalDate(), UTC, any()
            )
        } returns EventsDTO(emptyList())

        // confirm meeting
        asInitiator {
            val meetingProposal = underTest.loadMeetingProposal(meetingInvitationDTO.meetingCode)
            assertThat(meetingProposal.isOwnMeeting).isTrue()

            underTest.confirmMeeting(
                meetingInvitationDTO.meetingCode,
                ConfirmMeetingCommand(
                    meetingProposal.proposedSlots[0],
                    recipient().email
                )
            )
        }

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }

    @Test
    fun `claim sama-non-sama meeting and connect`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id,
                    SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(
                    60, clock.zone,
                )
            )
        }

        // propose meeting
        val now = ZonedDateTime.now(clock)
        val proposedSlot = MeetingSlotDTO(now.plusHours(2), now.plusHours(3))
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }


        // Connect with initiator and propose new slots
        every {
            userConnectionService.createUserConnection(recipient().id, CreateUserConnectionCommand(initiator().id))
        } returns true

        val meetingCode = meetingInvitationDTO.meetingCode
        val newProposedSlot = MeetingSlotDTO(now.plusHours(3), now.plusHours(4))
        asRecipient {
            underTest.connectWithInitiator(
                meetingInvitationDTO.meetingCode,
                ConnectWithMeetingInitiatorCommand
            )

            underTest.proposeNewMeetingSlots(
                meetingCode, ProposeNewMeetingSlotsCommand(listOf(newProposedSlot))
            )
        }

        // confirm meeting
        asInitiator {
            underTest.confirmMeeting(
                meetingInvitationDTO.meetingCode, ConfirmMeetingCommand(newProposedSlot, null)
            )
        }

        verify { calendarEventConsumer.onMeetingConfirmed(any()) }
        verify { commsEventConsumer.onMeetingConfirmed(any()) }
    }

    @Test
    fun `load meeting proposal from non-existent code`() {
        assertThrows<NotFoundException> {
            underTest.loadMeetingProposal(MeetingCode("VGsUTGno"))
        }
    }

    // @Test
    fun `expired meeting cannot be confirmed`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            every {
                slotSuggestionService.suggestSlots(
                    it.id,
                    SlotSuggestionRequest(ofMinutes(60), 3, clock.zone)
                )
            }.returns(
                SlotSuggestionResponse(
                    listOf(
                        SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)
                    )
                )
            )

            underTest.initiateSamaNonSamaMeeting(
                it.id, InitiateSamaNonSamaMeetingCommand(
                    60, clock.zone,
                )
            )
        }

        // propose meeting with one slot in 5 minutes
        val proposedSlotStart = ZonedDateTime.now(clock).plusMinutes(5)
        val proposedSlotEnd = proposedSlotStart.plusMinutes(65)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        // trigger expiration job
        underTest.expireMeetings()

        assertThrows<NotFoundException> {
            underTest.loadMeetingProposal(meetingInvitationDTO.meetingCode)
        }
    }
}