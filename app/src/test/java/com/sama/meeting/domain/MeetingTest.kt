package com.sama.meeting.domain

import com.sama.users.domain.UserId
import java.time.Duration.ofHours
import java.time.Duration.ofMinutes
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

class MeetingTest {
    private val validMeetingCode = MeetingCode("VGsUTGno")
    private val meetingIntentId = MeetingIntentId(1)
    private val meetingTitle = "meeting title"
    private val meetingId = MeetingId(11)
    private val initiatorId = UserId(21)

    private val _9am = ZonedDateTime.of(LocalDate.now(), LocalTime.of(9, 0), systemDefault())
    private val _915am = _9am.plus(ofMinutes(15))
    private val _930am = _9am.plus(ofMinutes(30))
    private val _945am = _9am.plus(ofMinutes(45))
    private val _10am = _9am.plus(ofHours(1))
    private val _11am = _9am.plus(ofHours(2))
    private val _12pm = _9am.plus(ofHours(3))

    private val suggestedSlotID1 = MeetingSlot(_9am, _10am)
    private val suggestedSlotID2 = MeetingSlot(_10am, _11am)
    private val suggestedSlotID3 = MeetingSlot(_11am, _12pm)
    private val proposedSlotID1 = MeetingSlot(_9am, _10am)
    private val proposedSlotID2 = MeetingSlot(_10am, _11am)
    private val proposedSlotID3 = MeetingSlot(_11am, _12pm)

    private val proposedMeetingID1 = ProposedMeeting(
        meetingId, meetingIntentId, ofMinutes(30), initiatorId,
        null, null, Actor.RECIPIENT, listOf(proposedSlotID1),
        emptyList(), validMeetingCode, meetingTitle
    )

    @TestFactory
    fun `initiate meeting with durations`() = listOf(
        ofMinutes(30) to true,
        ofMinutes(45) to true,
        ofMinutes(60) to true,
        ofMinutes(14) to false,
        ofMinutes(0) to false
    )
        .map { (duration, success) ->
            dynamicTest("meeting with duration '${duration.toMinutes()}min' allowed: $success") {
                if (success) {
                    MeetingIntent(meetingIntentId, initiatorId, duration, null, systemDefault(), emptyList())
                } else {
                    assertThrows(InvalidDurationException::class.java) {
                        MeetingIntent(meetingIntentId, initiatorId, duration, null, systemDefault(), emptyList())
                    }
                }
            }
        }


    @TestFactory
    fun `initiate 60 min meeting with a suggested slot`() = listOf(
        ofMinutes(60) to true,
        ofMinutes(30) to false,
        ofMinutes(61) to true
    )
        .map { (slotDuration, success) ->
            dynamicTest("suggesting a slot of ${slotDuration.toMinutes()}min allowed: $success") {
                val slot = MeetingSlot(_9am, _9am.plus(slotDuration))
                if (success) {
                    MeetingIntent(meetingIntentId, initiatorId, ofHours(1), null, systemDefault(), listOf(slot))
                } else {
                    assertThrows(InvalidMeetingSlotException::class.java) {
                        MeetingIntent(meetingIntentId, initiatorId, ofHours(1), null, systemDefault(), listOf(slot))
                    }
                }
            }
        }

    @TestFactory
    fun `propose meeting`(): List<DynamicTest> {
        val initiatorId = initiatorId
        val meetingIntentId = MeetingIntentId(2)
        val meetingTitle = meetingTitle
        return listOf(
            listOf(proposedSlotID1.copy()) to ProposedMeeting(
                meetingId, meetingIntentId, ofHours(1), initiatorId, null,
                UTC, Actor.RECIPIENT, listOf(proposedSlotID1), emptyList(), validMeetingCode, meetingTitle
            ),

            listOf(proposedSlotID1.copy(), proposedSlotID3.copy()) to ProposedMeeting(
                meetingId,
                meetingIntentId,
                ofHours(1),
                initiatorId,
                null,
                UTC,
                Actor.RECIPIENT,
                listOf(proposedSlotID1, proposedSlotID3), emptyList(),
                validMeetingCode,
                meetingTitle
            ),

            listOf(proposedSlotID1.copy(), proposedSlotID2.copy(), proposedSlotID3.copy()) to ProposedMeeting(
                meetingId,
                meetingIntentId,
                ofHours(1),
                initiatorId,
                null,
                UTC,
                Actor.RECIPIENT,
                listOf(MeetingSlot(proposedSlotID1.startDateTime, proposedSlotID3.endDateTime)),
                emptyList(),
                validMeetingCode,
                meetingTitle
            ),

            ).map { (proposedSlots, expected) ->
            val initiatedMeeting = MeetingIntent(
                meetingIntentId,
                initiatorId,
                ofHours(1),
                null,
                UTC,
                listOf(suggestedSlotID1, suggestedSlotID2),
            )

            dynamicTest("proposing slots $proposedSlots throws $expected") {
                val proposedMeeting = initiatedMeeting.propose(meetingId, validMeetingCode, proposedSlots, meetingTitle)

                assertEquals(expected, proposedMeeting)
            }
        }
    }

    @TestFactory
    fun `expand meeting slots`() = listOf(
        proposedMeetingID1.copy(proposedSlots = listOf(MeetingSlot(_9am, _10am))) to
                listOf(
                    MeetingSlot(_9am, _930am),
                    MeetingSlot(_915am, _945am),
                    MeetingSlot(_930am, _10am),
                ),
        proposedMeetingID1.copy(proposedSlots = listOf(MeetingSlot(_9am, _930am))) to
                listOf(MeetingSlot(_9am, _930am)),
    ).map { (proposedMeeting, expected) ->
        dynamicTest("${proposedMeeting.proposedSlots} expanded") {
            val actual = proposedMeeting.expandedSlots()
            assertEquals(expected, actual)
        }
    }

    @TestFactory
    fun `propose meeting fails`() = listOf(
        emptyList<MeetingSlot>() to InvalidMeetingProposalException::class.java,
        listOf(MeetingSlot(_9am, _930am)) to InvalidMeetingSlotException::class.java,
    ).map { (proposedSlots, expected) ->

        val initiatedMeeting = MeetingIntent(
            meetingIntentId,
            initiatorId,
            ofHours(1),
            null,
            UTC,
            listOf(proposedSlotID1.copy(), proposedSlotID2.copy())
        )
        val meetingCode = MeetingCode("VGsUTGno")

        dynamicTest("proposing slots $proposedSlots throws $expected") {
            assertThrows(expected) {
                initiatedMeeting.propose(meetingId, meetingCode, proposedSlots, meetingTitle)
            }
        }
    }

    @Test
    fun `propose new slots`() {
        val proposedMeeting = ProposedMeeting(
            meetingId, meetingIntentId, ofHours(1), initiatorId, null, null,
            Actor.RECIPIENT, listOf(proposedSlotID1), emptyList(), validMeetingCode, meetingTitle
        )

        val newProposedMeeting = proposedMeeting.proposeNewSlots(listOf(proposedSlotID2, proposedSlotID3))

        assertThat(newProposedMeeting.rejectedSlots).containsExactly(proposedSlotID1)
        assertThat(newProposedMeeting.proposedSlots).containsExactly(proposedSlotID2, proposedSlotID3)
    }

    @Test
    fun `propose previously rejected slots`() {
        val proposedMeeting = ProposedMeeting(
            meetingId, meetingIntentId, ofHours(1), initiatorId, null, null,
            Actor.RECIPIENT, listOf(proposedSlotID1), emptyList(), validMeetingCode, meetingTitle
        )

        val newProposedMeeting = proposedMeeting
            .proposeNewSlots(listOf(proposedSlotID2, proposedSlotID3))
            .proposeNewSlots(listOf(proposedSlotID1))

        assertThat(newProposedMeeting.rejectedSlots).containsExactly(proposedSlotID2, proposedSlotID3)
        assertThat(newProposedMeeting.proposedSlots).containsExactly(proposedSlotID1)
    }

    @Test
    fun `confirm meeting`() {
        val slot = proposedSlotID1
        val meetingTitle = meetingTitle
        val proposedMeeting = ProposedMeeting(
            meetingId, meetingIntentId, ofHours(1), initiatorId, null, null,
            Actor.RECIPIENT, listOf(slot), emptyList(), validMeetingCode, meetingTitle
        )
        val recipient = UserRecipient.of(UserId(11L), "recipient@meetsama.com")
        val slotToConfirm = proposedSlotID1.copy()

        val actual = proposedMeeting.confirm(slotToConfirm, recipient)

        assertEquals(
            ConfirmedMeeting(
                meetingId, initiatorId, ofHours(1), recipient,
                slotToConfirm, meetingTitle
            ), actual
        )
    }

    @Test
    fun `confirm non-proposed slot fails`() {
        val slot = proposedSlotID1
        val proposedMeeting = ProposedMeeting(
            meetingId,
            meetingIntentId,
            ofHours(1),
            initiatorId,
            null,
            null,
            Actor.RECIPIENT,
            listOf(slot),
            emptyList(),
            validMeetingCode,
            meetingTitle
        )
        val recipient = EmailRecipient.of("test@meetsama.com")

        val slotToConfirm = proposedSlotID2.copy()

        assertThrows<MeetingSlotUnavailableException> {
            proposedMeeting.confirm(slotToConfirm, recipient)
        }
    }
}