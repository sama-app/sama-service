package com.sama.meeting.application

import com.sama.calendar.application.CalendarEventConsumer
import com.sama.calendar.application.EventApplicationService
import com.sama.calendar.application.FetchEventsDTO
import com.sama.common.BaseApplicationIntegrationTest
import com.sama.common.NotFoundException
import com.sama.comms.application.CommsEventConsumer
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingConfirmedEvent
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionResponse
import com.sama.slotsuggestion.application.SlotSuggestionService
import com.sama.slotsuggestion.domain.SlotSuggestion
import java.time.Clock
import java.time.Duration.ofMinutes
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.access.AccessDeniedException

class MeetingApplicationServiceIT : BaseApplicationIntegrationTest() {

    @MockBean
    lateinit var slotSuggestionService: SlotSuggestionService

    @MockBean
    lateinit var eventApplicationService: EventApplicationService

    @MockBean
    lateinit var calendarEventConsumer: CalendarEventConsumer

    @MockBean
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
            whenever(
                slotSuggestionService.suggestSlots(
                    it.id!!, SlotSuggestionRequest(ofMinutes(60), clock.zone, 3)
                )
            ).thenReturn(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateMeeting(
                it.id!!, InitiateMeetingCommand(
                    60, clock.zone, 3
                )
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                it.id!!,
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        // load proposal from meeting code with initiator's calendar non-blocked
        whenever(
            eventApplicationService.fetchEvents(
                initiator().id!!,
                proposedSlotStart.toLocalDate(),
                proposedSlotEnd.toLocalDate(),
                proposedSlotStart.zone
            )
        ).thenReturn(FetchEventsDTO(emptyList(), emptyList()))

        val meetingProposal = underTest.loadMeetingProposal(null, meetingInvitationDTO.meetingCode)

        // confirm meeting
        underTest.confirmMeeting(
            null,
            meetingInvitationDTO.meetingCode,
            ConfirmMeetingCommand(
                meetingProposal.proposedSlots[0],
                "non-sama-recipient@meetsama.com"
            )
        )

        verify(calendarEventConsumer).onMeetingConfirmed(any())
        verify(commsEventConsumer).onMeetingConfirmed(any())
    }

    @Test
    fun `only initiator can propose meetings from their own intent`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            whenever(
                slotSuggestionService.suggestSlots(
                    it.id!!,
                    SlotSuggestionRequest(ofMinutes(60), clock.zone, 3)
                )
            ).thenReturn(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateMeeting(
                it.id!!, InitiateMeetingCommand(
                    60, clock.zone, 3
                )
            )
        }

        assertThrows<AccessDeniedException> {
            asRecipient {
                underTest.proposeMeeting(
                    it.id!!,
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
            whenever(
                slotSuggestionService.suggestSlots(
                    it.id!!, SlotSuggestionRequest(ofMinutes(60), clock.zone, 3)
                )
            ).thenReturn(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateMeeting(
                it.id!!, InitiateMeetingCommand(60, clock.zone, 3)
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                it.id!!,
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        // update title
        val meetingTitle = "My new fancy title"
        asInitiator {
            underTest.updateMeetingTitle(it.id!!, meetingInvitationDTO.meetingCode,
                UpdateMeetingTitleCommand(meetingTitle))
        }

        // load proposal from meeting code with initiator's calendar non-blocked
        whenever(eventApplicationService.fetchEvents(
            initiator().id!!,
            proposedSlotStart.toLocalDate(),
            proposedSlotEnd.toLocalDate(),
            proposedSlotStart.zone
        )).thenReturn(FetchEventsDTO(emptyList(), emptyList()))

        val meetingProposal = underTest.loadMeetingProposal(null, meetingInvitationDTO.meetingCode)
        assertThat(meetingProposal.title).isEqualTo(meetingTitle) // verify new title is here

        // confirm meeting
        underTest.confirmMeeting(
            null,
            meetingInvitationDTO.meetingCode,
            ConfirmMeetingCommand(
                meetingProposal.proposedSlots[0],
                "non-sama-recipient@meetsama.com"
            )
        )

        argumentCaptor<MeetingConfirmedEvent> {
            verify(calendarEventConsumer).onMeetingConfirmed(capture())
            verify(commsEventConsumer).onMeetingConfirmed(capture())
            assertThat(firstValue).isEqualTo(secondValue)

            assertThat(firstValue.confirmedMeeting.meetingTitle)
                .isEqualTo(meetingTitle)
        }
    }

//    @Test
//    fun `setup sama-non-sama meeting when all initiators proposed slots are blocked`() {
//        // create meeting intent
//        val meetingIntentDTO = asInitiator {
//            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
//            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
//            whenever(
//                slotSuggestionService.suggestSlots(
//                    it.id!!,
//                    SlotSuggestionRequest(ofMinutes(60), clock.zone, 3)
//                )
//            ).thenReturn(
//                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
//            )
//
//            underTest.initiateMeeting(
//                it.id!!, InitiateMeetingCommand(
//                    60, clock.zone, 3
//                )
//            )
//        }
//
//        // propose meeting with two slots
//        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
//        val proposedSlotEnd = proposedSlotStart.plusHours(3)
//        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
//
//        val proposedSlotTwoStart = ZonedDateTime.now(clock).plusDays(1).plusHours(2)
//        val proposedSlotTwoEnd = proposedSlotTwoStart.plusDays(1).plusHours(3)
//        val proposedSlotTwo = MeetingSlotDTO(proposedSlotTwoStart, proposedSlotTwoEnd)
//        val meetingInvitationDTO = asInitiator {
//            underTest.proposeMeeting(
//                it.id!!,
//                ProposeMeetingCommand(
//                    meetingIntentDTO.meetingIntentCode,
//                    listOf(proposedSlot, proposedSlotTwo)
//                )
//            )
//        }
//
//        // load proposal from meeting code with initiator's calendar blocked completely
//        whenever(
//            eventApplicationService.fetchEvents(
//                initiator().id!!,
//                proposedSlotStart.toLocalDate(),
//                proposedSlotTwoEnd.toLocalDate(),
//                proposedSlotStart.zone
//            )
//        ).thenReturn(
//            FetchEventsDTO(emptyList(), listOf(EventDTO(proposedSlotStart, proposedSlotTwoEnd, false, "Title")))
//        )
//        val meetingProposal = underTest.loadMeetingProposal(null, meetingInvitationDTO.meetingCode)
//
//        // no proposed slots available
//        assertThat(meetingProposal.proposedSlots).isEmpty()
//
//        // try to confirm meeting
//        assertThrows<MeetingSlotUnavailableException> {
//            underTest.confirmMeeting(
//                null,
//                meetingInvitationDTO.meetingCode,
//                ConfirmMeetingCommand(
//                    proposedSlot,
//                    recipient().email
//                )
//            )
//        }
//
//        verifyZeroInteractions(calendarEventConsumer)
//        verifyZeroInteractions(commsEventConsumer)
//    }

    @Test
    fun `setup sama-non-sama meeting with existing sama user`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            whenever(
                slotSuggestionService.suggestSlots(
                    it.id!!,
                    SlotSuggestionRequest(ofMinutes(60), clock.zone, 3)
                )
            ).thenReturn(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateMeeting(
                it.id!!, InitiateMeetingCommand(
                    60, clock.zone, 3
                )
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                it.id!!,
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        // load proposal from meeting code with initiator's calendar non-blocked
        whenever(
            eventApplicationService.fetchEvents(
                initiator().id!!,
                proposedSlotStart.toLocalDate(),
                proposedSlotEnd.toLocalDate(),
                proposedSlotStart.zone
            )
        )
            .thenReturn(FetchEventsDTO(emptyList(), emptyList()))

        // confirm meeting
        asRecipient {
            val meetingProposal = underTest.loadMeetingProposal(recipient().id!!, meetingInvitationDTO.meetingCode)
            assertThat(meetingProposal.isOwnMeeting).isFalse()

            underTest.confirmMeeting(
                recipient().id!!,
                meetingInvitationDTO.meetingCode,
                ConfirmMeetingCommand(
                    meetingProposal.proposedSlots[0],
                    null
                )
            )
        }

        verify(calendarEventConsumer).onMeetingConfirmed(any())
        verify(commsEventConsumer).onMeetingConfirmed(any())
    }


    @Test
    fun `setup sama-non-sama meeting with yourself`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            whenever(
                slotSuggestionService.suggestSlots(
                    it.id!!,
                    SlotSuggestionRequest(ofMinutes(60), clock.zone, 3)
                )
            ).thenReturn(
                SlotSuggestionResponse(listOf(SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)))
            )

            underTest.initiateMeeting(
                it.id!!, InitiateMeetingCommand(
                    60, clock.zone, 3
                )
            )
        }

        // propose meeting
        val proposedSlotStart = ZonedDateTime.now(clock).plusHours(2)
        val proposedSlotEnd = proposedSlotStart.plusHours(3)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                it.id!!,
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        // load proposal from meeting code with initiator's calendar non-blocked
        whenever(
            eventApplicationService.fetchEvents(
                initiator().id!!,
                proposedSlotStart.toLocalDate(),
                proposedSlotEnd.toLocalDate(),
                proposedSlotStart.zone
            )
        )
            .thenReturn(FetchEventsDTO(emptyList(), emptyList()))

        // confirm meeting
        asInitiator {
            val meetingProposal = underTest.loadMeetingProposal(initiator().id!!, meetingInvitationDTO.meetingCode)
            assertThat(meetingProposal.isOwnMeeting).isTrue()

            underTest.confirmMeeting(
                initiator().id!!,
                meetingInvitationDTO.meetingCode,
                ConfirmMeetingCommand(
                    meetingProposal.proposedSlots[0],
                    recipient().email
                )
            )
        }

        verify(calendarEventConsumer).onMeetingConfirmed(any())
        verify(commsEventConsumer).onMeetingConfirmed(any())
    }

    @Test
    fun `load meeting proposal from non-existent code`() {
        assertThrows<NotFoundException> {
            underTest.loadMeetingProposal(null, MeetingCode("VGsUTGno"))
        }
    }

    // @Test
    fun `expired meeting cannot be confirmed`() {
        // create meeting intent
        val meetingIntentDTO = asInitiator {
            val suggestedSlotStart = ZonedDateTime.now(clock).plusHours(1)
            val suggestedSlotEnd = suggestedSlotStart.plusHours(2)
            whenever(
                slotSuggestionService.suggestSlots(
                    it.id!!,
                    SlotSuggestionRequest(ofMinutes(60), clock.zone, 3)
                )
            )
                .thenReturn(
                    SlotSuggestionResponse(
                        listOf(
                            SlotSuggestion(suggestedSlotStart, suggestedSlotEnd, 1.0)
                        )
                    )
                )

            underTest.initiateMeeting(
                it.id!!, InitiateMeetingCommand(
                    60, clock.zone, 3
                )
            )
        }

        // propose meeting with one slot in 5 minutes
        val proposedSlotStart = ZonedDateTime.now(clock).plusMinutes(5)
        val proposedSlotEnd = proposedSlotStart.plusMinutes(65)
        val proposedSlot = MeetingSlotDTO(proposedSlotStart, proposedSlotEnd)
        val meetingInvitationDTO = asInitiator {
            underTest.proposeMeeting(
                it.id!!,
                ProposeMeetingCommand(
                    meetingIntentDTO.meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        }

        // trigger expiration job
        underTest.expireMeetings()

        assertThrows<NotFoundException> {
            underTest.loadMeetingProposal(null, meetingInvitationDTO.meetingCode)
        }
    }
}