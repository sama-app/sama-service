package com.sama.calendar.domain

import com.sama.calendar.domain.MeetingSlotStatus.*
import com.sama.common.NotFoundException
import com.sama.common.assertDoesNotThrowOrNull
import com.sama.common.assertThrows
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.*
import java.time.Duration.ofHours
import java.time.Duration.ofMinutes
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeetingsTest {
    private val validMeetingCode = "valid-meet-code"
    private val initiatorId = 11L

    private val _9am = ZonedDateTime.of(LocalDate.now(), LocalTime.of(9, 0), ZoneId.systemDefault())
    private val _10am = _9am.plus(ofHours(1))
    private val _11am = _9am.plus(ofHours(2))
    private val _12pm = _9am.plus(ofHours(3))

    private val suggestedSlotID1 = MeetingSlot(1L, SUGGESTED, _9am, _10am)
    private val suggestedSlotID2 = MeetingSlot(2L, SUGGESTED, _10am, _11am)
    private val suggestedSlotID3 = MeetingSlot(3L, SUGGESTED, _11am, _12pm)
    private val proposedSlotID1 = MeetingSlot(1L, PROPOSED, _9am, _10am)
    private val proposedSlotID2 = MeetingSlot(2L, PROPOSED, _10am, _11am)

    @Test
    fun `initiate meeting has status INITIATED`() {
        val status = InitiatedMeeting(1L, initiatorId, ofHours(1), emptyList(), null).status
        assertEquals(MeetingStatus.INITIATED, status)
    }


    @TestFactory
    fun `initiate meeting with durations`() = listOf(
        ofMinutes(30) to true,
        ofMinutes(45) to true,
        ofMinutes(60) to true,
        ofMinutes(15) to false,
        ofMinutes(57) to false,
        ofMinutes(82) to false
    )
        .map { (duration, success) ->
            dynamicTest("meeting with duration '${duration.toMinutes()}min' allowed: $success") {
                if (success) {
                    InitiatedMeeting(1L, 10L, duration, emptyList(), null)
                } else {
                    assertThrows(UnsupportedDurationException::class.java) {
                        InitiatedMeeting(1L, 10L, duration, emptyList(), null)
                    }
                }
            }
        }


    @TestFactory
    fun `initiate 60 min meeting with a suggested slot`() = listOf(
        ofMinutes(60) to true,
        ofMinutes(30) to true,
        ofMinutes(61) to false
    )
        .map { (slotDuration, success) ->
            dynamicTest("suggesting a slot of ${slotDuration.toMinutes()}min allowed: $success") {
                val slot = MeetingSlot.new(initiatorId, _9am, _9am.plus(slotDuration))
                if (success) {
                    InitiatedMeeting(1L, 10L, ofHours(1), listOf(slot), null)
                } else {
                    assertThrows(InvalidSuggestedSlotException::class.java) {
                        InitiatedMeeting(1L, 10L, ofHours(1), listOf(slot), null)
                    }
                }
            }
        }


    @TestFactory
    fun `suggest slots to 60 min meeting`() = listOf(
        ofMinutes(60) to true,
        ofMinutes(30) to true,
        ofMinutes(61) to false
    )
        .map { (slotDuration, success) ->
            val initiatedMeeting = InitiatedMeeting(1L, 10L, ofHours(1), emptyList(), null)
            dynamicTest("suggesting a slot of ${slotDuration.toMinutes()}min allowed: $success") {
                val slot = MeetingSlot.new(initiatorId, _9am, _9am.plus(slotDuration))
                val result = initiatedMeeting.suggestSlots(listOf(slot))

                if (success) {
                    val actual = result.assertDoesNotThrowOrNull()
                    assertContains(actual.suggestedSlots, slot)
                } else {
                    result.assertThrows(InvalidSuggestedSlotException::class.java)
                }
            }
        }

    @TestFactory
    fun `remove initiated meeting slot`() = listOf(
        suggestedSlotID1 to true,
        suggestedSlotID2 to true,
        suggestedSlotID3 to false // doesn't exist
    ).map { (slot, success) ->

        val initiatedMeeting = InitiatedMeeting(
            1L, initiatorId, ofHours(1),
            listOf(suggestedSlotID1, suggestedSlotID2), null
        )

        dynamicTest("removing a slot#${slot.meetingSlotId} from a meeting allowed: $success") {
            val result = initiatedMeeting.removeSlot(slot.meetingSlotId)

            if (success) {
                val actual = result.assertDoesNotThrowOrNull()
                assertFalse(slot in actual.suggestedSlots)
                assertContains(actual.suggestedSlots, slot.copy(status = REMOVED))
            } else {
                result.assertThrows(NotFoundException::class.java)
            }
        }
    }

    @TestFactory
    fun `propose meeting`(): List<DynamicTest> {
        val initiatorId = initiatorId
        return listOf(
            setOf(1L) to ProposedMeeting(
                1L, initiatorId, ofHours(1), listOf(proposedSlotID1),
                null, validMeetingCode
            ),

            setOf(1L, 2L) to ProposedMeeting(
                1L, initiatorId, ofHours(1), listOf(proposedSlotID1, proposedSlotID2),
                null, validMeetingCode
            ),

            ).map { (proposedSlotIds, expected) ->
            val initiatedMeeting = InitiatedMeeting(
                1L, initiatorId, ofHours(1), listOf(suggestedSlotID1, suggestedSlotID2), null
            )

            dynamicTest("proposing slots $proposedSlotIds throws $expected") {
                val proposedMeeting = initiatedMeeting.propose(proposedSlotIds, validMeetingCode)

                val actual = proposedMeeting.assertDoesNotThrowOrNull()
                assertEquals(expected, actual)
            }
        }
    }

    @TestFactory
    fun `propose meeting fails`() = listOf(
        emptySet<SlotId>() to InvalidMeetingProposalException::class.java,
        setOf(3L) to NotFoundException::class.java,
        setOf(1L, 4L) to NotFoundException::class.java,
    ).map { (proposedSlotIds, expected) ->

        val initiatedMeeting = InitiatedMeeting(
            1L, initiatorId, ofHours(1),
            listOf(proposedSlotID1, proposedSlotID2), null
        )
        val meetingCode = "meet-me-kei"

        dynamicTest("proposing slots $proposedSlotIds throws $expected") {
            val actual = initiatedMeeting.propose(proposedSlotIds, meetingCode)
            actual.assertThrows(expected)
        }
    }

    @Test
    fun `confirm meeting`() {
        val slot = proposedSlotID1
        val proposedMeeting = ProposedMeeting(
            1L, initiatorId, ofHours(1), listOf(slot), null, validMeetingCode
        )
        val recipient = MeetingRecipient(null, null) // todo

        val actual = proposedMeeting.confirm(1L, recipient)

        assertTrue(actual.isSuccess)
        assertEquals(
            ConfirmedMeeting(
                1L, initiatorId, ofHours(1), recipient,
                MeetingSlot(1L, MeetingSlotStatus.CONFIRMED, _9am, _10am)
            ), actual.getOrNull()
        )
    }


    @Test
    fun `confirm meeting fails`() {
        val slot = proposedSlotID1
        val proposedMeeting = ProposedMeeting(
            1L, initiatorId, ofHours(1), listOf(slot), null, validMeetingCode
        )
        val recipient = MeetingRecipient(null, null) // todo

        val actual = proposedMeeting.confirm(2L, recipient)

        actual.assertThrows(NotFoundException::class.java)
    }
}