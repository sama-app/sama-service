package com.sama.meeting.application

import com.sama.integration.firebase.DynamicLinkService
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.Actor
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.application.UserPublicDTO
import com.sama.users.application.UserService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val scheme = "https"
private const val host = "sama.com"
private val meetingCode = MeetingCode("VGsUTGno")

@ExtendWith(MockitoExtension::class)
class MeetingViewTest {
    private val urlConfiguration = MeetingUrlConfiguration(10, scheme, host)

    @Mock
    lateinit var userService: UserService

    @Mock
    lateinit var dynamicLinkService: DynamicLinkService

    lateinit var underTest: MeetingView

    @BeforeEach
    fun setup() {
        underTest = MeetingView(
            userService,
            dynamicLinkService,
            urlConfiguration)
    }

    @Test
    fun render() {
        val _9am = ZonedDateTime.of(
            LocalDate.of(2021, 7, 7),
            LocalTime.of(9, 0), ZoneId.of("UTC")
        )
        val _10am = _9am.plusHours(1)
        val _11am = _9am.plusHours(2)

        val currentUserId = UserId(10)
        val initiatorId = UserId(1)
        val initiator = UserPublicDTO(UserPublicId.random(), "test", "test@meetsama.com")
        whenever(userService.findAll(listOf(initiatorId)))
            .thenReturn(mapOf(initiatorId to initiator))
        val meetingTitle = "Meeting with test"

        val dynamicUrl = "https://meetsamatest.page.link/dynamic"
        whenever(dynamicLinkService.generate(anyString(), anyString()))
            .thenReturn(dynamicUrl)

        // act
        val proposedSlots = listOf(
            MeetingSlot(_9am, _9am.plusMinutes(15)),
            MeetingSlot(_10am, _11am)
        )
        val slots = listOf(MeetingSlot(_10am, _11am))
        val actual = underTest.render(
            currentUserId,
            ProposedMeeting(
                MeetingId(21), MeetingIntentId(11), Duration.ofMinutes(15),
                initiatorId,
                null,
                null,
                Actor.RECIPIENT,
                proposedSlots,
                emptyList(),
                meetingCode,
                meetingTitle
            ),
            slots
        )

        // verify
        val expectedUrl = "$scheme://$host/${meetingCode.code}"
        val expected = ProposedMeetingDTO(
            proposedSlots = listOf(MeetingSlotDTO(_10am, _11am)),
            initiator = UserPublicDTO(
                initiator.userId,
                initiator.fullName,
                initiator.email
            ),
            recipient = null,
            isReadOnly = false,
            isOwnMeeting = false,
            title = meetingTitle,
            appLinks = MeetingAppLinksDTO(dynamicUrl)
        )
        verify(dynamicLinkService).generate(meetingCode.code, expectedUrl)

        assertEquals(expected, actual)
    }

    @Test
    fun `render own meeting`() {
        val currentUserId = UserId(1)
        val initiatorId = UserId(1)
        val initiator = UserPublicDTO(UserPublicId.random(), "test", "test@meetsama.com")
        whenever(userService.findAll(listOf(initiatorId)))
            .thenReturn(mapOf(initiatorId to initiator))
        val meetingTitle = "Meeting with test"

        val dynamicUrl = "https://meetsamatest.page.link/dynamic"
        whenever(dynamicLinkService.generate(anyString(), anyString()))
            .thenReturn(dynamicUrl)

        // act
        val actual = underTest.render(
            currentUserId,
            ProposedMeeting(
                MeetingId(21), MeetingIntentId(11), Duration.ofMinutes(15),
                initiatorId,
                null,
                null,
                Actor.RECIPIENT,
                emptyList(),
                emptyList(),
                meetingCode,
                meetingTitle
            ),
            emptyList()
        )

        // verify
        val expectedUrl = "$scheme://$host/${meetingCode.code}"
        val expected = ProposedMeetingDTO(
            proposedSlots = emptyList(),
            initiator = UserPublicDTO(
                initiator.userId,
                initiator.fullName,
                initiator.email
            ),
            recipient = null,
            isReadOnly = false,
            isOwnMeeting = true,
            title = meetingTitle,
            appLinks = MeetingAppLinksDTO(dynamicUrl)
        )
        verify(dynamicLinkService).generate(meetingCode.code, expectedUrl)

        assertEquals(expected, actual)
    }
}