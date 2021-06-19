package com.sama.meeting.domain

import com.sama.common.assertDoesNotThrowOrNull
import com.sama.common.assertThrows
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Duration.ofHours
import java.time.Duration.ofMinutes
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId.systemDefault
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MeetingTest {
    private val validMeetingCode = "valid-meet-code"
    private val initiatorId = 11L

    private val _9am = ZonedDateTime.of(LocalDate.now(), LocalTime.of(9, 0), systemDefault())
    private val _930am = _9am.plus(ofMinutes(30))
    private val _10am = _9am.plus(ofHours(1))
    private val _11am = _9am.plus(ofHours(2))
    private val _12pm = _9am.plus(ofHours(3))

    private val suggestedSlotID1 = MeetingSlot(_9am, _10am)
    private val suggestedSlotID2 = MeetingSlot(_10am, _11am)
    private val suggestedSlotID3 = MeetingSlot(_11am, _12pm)
    private val proposedSlotID1 = MeetingSlot(_9am, _10am)
    private val proposedSlotID2 = MeetingSlot(_10am, _11am)
    private val proposedSlotID3 = MeetingSlot(_11am, _12pm)


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
                    MeetingIntent(1L, 10L, null, duration, systemDefault(), emptyList())
                } else {
                    assertThrows(InvalidDurationException::class.java) {
                        MeetingIntent(1L, 10L, null, duration, systemDefault(), emptyList())
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
                    MeetingIntent(1L, 10L, null, ofHours(1), systemDefault(), listOf(slot))
                } else {
                    assertThrows(InvalidMeetingSlotException::class.java) {
                        MeetingIntent(1L, 10L, null, ofHours(1), systemDefault(), listOf(slot))
                    }
                }
            }
        }

    @TestFactory
    fun `propose meeting`(): List<DynamicTest> {
        val initiatorId = initiatorId
        val meetingId = 11L
        val meetingIntentId = 2L
        return listOf(
            listOf(proposedSlotID1.copy()) to ProposedMeeting(
                meetingId, meetingIntentId, initiatorId, ofHours(1),
                listOf(proposedSlotID1), validMeetingCode
            ),

            listOf(proposedSlotID1.copy(), proposedSlotID2.copy()) to ProposedMeeting(
                meetingId, meetingIntentId, initiatorId, ofHours(1),
                listOf(proposedSlotID1, proposedSlotID2), validMeetingCode
            ),

            ).map { (proposedSlots, expected) ->
            val initiatedMeeting = MeetingIntent(
                meetingIntentId,
                initiatorId,
                null,
                ofHours(1),
                systemDefault(),
                listOf(suggestedSlotID1, suggestedSlotID2),
            )

            dynamicTest("proposing slots $proposedSlots throws $expected") {
                val proposedMeeting = initiatedMeeting.propose(meetingId, validMeetingCode, proposedSlots)

                val actual = proposedMeeting.assertDoesNotThrowOrNull()
                assertEquals(expected, actual)
            }
        }
    }

    @TestFactory
    fun `propose meeting fails`() = listOf(
        emptyList<MeetingSlot>() to InvalidMeetingProposalException::class.java,
        listOf(MeetingSlot(_9am, _930am)) to InvalidMeetingSlotException::class.java,
    ).map { (proposedSlots, expected) ->

        val initiatedMeeting = MeetingIntent(
            1L, initiatorId, null, ofHours(1), systemDefault(),
            listOf(proposedSlotID1.copy(), proposedSlotID2.copy())
        )
        val meetingCode = "meet-me-kei"

        dynamicTest("proposing slots $proposedSlots throws $expected") {
            val actual = initiatedMeeting.propose(11L, meetingCode, proposedSlots)
            actual.assertThrows(expected)
        }
    }

    @Test
    fun `confirm meeting`() {
        val meetingId = 11L
        val slot = proposedSlotID1
        val proposedMeeting = ProposedMeeting(
            meetingId, 1L, initiatorId, ofHours(1), listOf(slot), validMeetingCode
        )
        val recipient = MeetingRecipient(null, null) // todo
        val slotToConfirm = proposedSlotID1.copy()

        val actual = proposedMeeting.confirm(slotToConfirm, recipient)

        assertTrue(actual.isSuccess)
        assertEquals(
            ConfirmedMeeting(
                meetingId, initiatorId, ofHours(1), recipient,
                slotToConfirm
            ), actual.getOrNull()
        )
    }

    @Test
    fun `confirm meeting fails`() {
        val meetingId = 11L
        val slot = proposedSlotID1
        val proposedMeeting = ProposedMeeting(
            meetingId, 1L, initiatorId, ofHours(1), listOf(slot), validMeetingCode
        )
        val recipient = MeetingRecipient(null, null) // todo

        val slotToConfirm = proposedSlotID2.copy()
        val actual = proposedMeeting.confirm(slotToConfirm, recipient)

        actual.assertThrows(MeetingSlotUnavailableException::class.java)
    }
}