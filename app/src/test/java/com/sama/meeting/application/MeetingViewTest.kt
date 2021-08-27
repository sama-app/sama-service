package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingAppLinkConfiguration
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.AvailableSlots
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension

private const val scheme = "https"
private const val host = "sama.com"
private val meetingCode = MeetingCode("VGsUTGno")

@TestConfiguration
class MeetingViewTestConfiguration {
    @Bean
    fun meetingUrlConfiguration(): MeetingUrlConfiguration =
        MeetingUrlConfiguration(10, scheme, host)

    @Bean
    fun meetingAppLinkConfiguration() =
        MeetingAppLinkConfiguration(
            "meetsamatest.page.link",
            mapOf("param1" to "value1", "param2" to "value2", "emptyVal" to "")
        )
}

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        MeetingView::class,
        MeetingViewTestConfiguration::class
    ]
)
class MeetingViewTest {
    @MockBean
    lateinit var userService: UserService

    @Autowired
    lateinit var underTest: MeetingView

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
        whenever(userService.find(initiatorId))
            .thenReturn(initiator)
        val meetingTitle = "Meeting with test"

        // act
        val proposedSlots = listOf(
            MeetingSlot(_9am, _9am.plusMinutes(15)),
            MeetingSlot(_10am, _11am)
        )
        val availableSlots = listOf(MeetingSlot(_10am, _11am))
        val actual = underTest.render(
            currentUserId,
            ProposedMeeting(
                MeetingId(21), MeetingIntentId(11), initiatorId,
                Duration.ofMinutes(15),
                proposedSlots,
                meetingCode,
                meetingTitle
            ),
            AvailableSlots(availableSlots)
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
            isOwnMeeting = false,
            title = meetingTitle,
            appLinks = MeetingAppLinksDTO(
                iosAppDownloadLink = "https://meetsamatest.page.link/?link=$expectedUrl&param1=value1&param2=value2"
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `render own meeting`() {
        val currentUserId = UserId(1)
        val initiatorId = UserId(1)
        val initiator = UserPublicDTO(UserPublicId.random(), "test", "test@meetsama.com")
        whenever(userService.find(initiatorId))
            .thenReturn(initiator)
        val meetingTitle = "Meeting with test"

        // act
        val actual = underTest.render(
            currentUserId,
            ProposedMeeting(
                MeetingId(21), MeetingIntentId(11), initiatorId,
                Duration.ofMinutes(15),
                emptyList(),
                meetingCode,
                meetingTitle
            ),
            AvailableSlots(emptyList())
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
            isOwnMeeting = true,
            title = meetingTitle,
            appLinks = MeetingAppLinksDTO(
                iosAppDownloadLink = "https://meetsamatest.page.link/?link=$expectedUrl&param1=value1&param2=value2"
            )
        )

        assertEquals(expected, actual)
    }
}