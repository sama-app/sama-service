package com.sama.calendar.domain

import com.sama.common.NotFoundException
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
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MeetingsTest {
    private val validMeetingCode = "valid-meet-code"
    private val initiatorId = 11L

    private val _9am = ZonedDateTime.of(LocalDate.now(), LocalTime.of(9, 0), ZoneId.systemDefault())
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
                    assertThrows(InvalidDurationException::class.java) {
                        InitiatedMeeting(1L, 10L, duration, emptyList(), null)
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
                    InitiatedMeeting(1L, 10L, ofHours(1), listOf(slot), null)
                } else {
                    assertThrows(InvalidMeetingSlotException::class.java) {
                        InitiatedMeeting(1L, 10L, ofHours(1), listOf(slot), null)
                    }
                }
            }
        }


    @TestFactory
    fun `suggest slots to 60 min meeting`() = listOf(
        ofMinutes(60) to true,
        ofMinutes(30) to false,
        ofMinutes(61) to true
    )
        .map { (slotDuration, success) ->
            val initiatedMeeting = InitiatedMeeting(1L, 10L, ofHours(1), emptyList(), null)
            dynamicTest("suggesting a slot of ${slotDuration.toMinutes()}min allowed: $success") {
                val slot = MeetingSlot(_9am, _9am.plus(slotDuration))
                val result = initiatedMeeting.suggestSlots(listOf(slot))

                if (success) {
                    val actual = result.assertDoesNotThrowOrNull()
                    assertContains(actual.suggestedSlots, slot)
                } else {
                    result.assertThrows(InvalidMeetingSlotException::class.java)
                }
            }
        }

    @TestFactory
    fun `propose meeting`(): List<DynamicTest> {
        val initiatorId = initiatorId
        return listOf(
            listOf(proposedSlotID1.copy()) to ProposedMeeting(
                1L, initiatorId, ofHours(1), listOf(proposedSlotID1),
                null, validMeetingCode
            ),

            listOf(proposedSlotID1.copy(), proposedSlotID2.copy()) to ProposedMeeting(
                1L, initiatorId, ofHours(1), listOf(proposedSlotID1, proposedSlotID2),
                null, validMeetingCode
            ),

            ).map { (proposedSlots, expected) ->
            val initiatedMeeting = InitiatedMeeting(
                1L, initiatorId, ofHours(1), listOf(suggestedSlotID1, suggestedSlotID2), null
            )

            dynamicTest("proposing slots $proposedSlots throws $expected") {
                val proposedMeeting = initiatedMeeting.propose(proposedSlots, validMeetingCode)

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

        val initiatedMeeting = InitiatedMeeting(
            1L, initiatorId, ofHours(1),
            listOf(proposedSlotID1.copy(), proposedSlotID2.copy()), null
        )
        val meetingCode = "meet-me-kei"

        dynamicTest("proposing slots $proposedSlots throws $expected") {
            val actual = initiatedMeeting.propose(proposedSlots, meetingCode)
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
        val slotToConfirm = proposedSlotID1.copy()

        val actual = proposedMeeting.confirm(slotToConfirm, recipient)

        assertTrue(actual.isSuccess)
        assertEquals(
            ConfirmedMeeting(
                1L, initiatorId, ofHours(1), recipient,
                slotToConfirm
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

        val slotToConfirm = proposedSlotID2.copy()
        val actual = proposedMeeting.confirm(slotToConfirm, recipient)

        actual.assertThrows(NotFoundException::class.java)
    }

    @Test
    fun `meeting code generated with length 10`() {
        assertTrue(MeetingCodeGenerator.default().generate().length == 10)
    }

    @Test
    fun `different meeting code generated each time`() {
        val generator1 = MeetingCodeGenerator.default()
        val generator2 = MeetingCodeGenerator.default()
        val code1 = generator1.generate()
        val code2 = generator1.generate()
        val code3 = generator2.generate()

        assertNotEquals(code1, code2)
        assertNotEquals(code2, code3)
    }
}