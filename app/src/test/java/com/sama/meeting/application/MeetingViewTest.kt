package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingAppLinkConfiguration
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.AvailableSlots
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.*
import java.util.*
import kotlin.test.assertEquals

private const val scheme = "https"
private const val host = "sama.com"
private const val meetingCode = "code"

@TestConfiguration
class MeetingViewTestConfiguration {
    @Bean
    fun meetingUrlConfiguration(): MeetingUrlConfiguration =
        MeetingUrlConfiguration(10, scheme, host)

    @Bean
    fun meetingAppLinkConfiguration() =
        MeetingAppLinkConfiguration("meetsamatest.page.link", mapOf("param1" to "value1", "param2" to "value2"))
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
    lateinit var userRepository: UserRepository

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

        val initiatorId = 1L
        val initiatorEntity = UserEntity("test@meetsama.com").apply { this.fullName = "test" }
        whenever(userRepository.findById(initiatorId))
            .thenReturn(Optional.of(initiatorEntity))

        // act
        val proposedSlots = listOf(
            MeetingSlot(_9am, _9am.plusMinutes(15)),
            MeetingSlot(_10am, _11am)
        )
        val availableSlots = listOf(MeetingSlot(_10am, _11am))
        val actual = underTest.render(
            ProposedMeeting(
                21L, 11L, initiatorId,
                Duration.ofMinutes(15),
                proposedSlots,
                meetingCode
            ),
            AvailableSlots(availableSlots)
        )

        // verify
        val expectedUrl = "$scheme://$host/$meetingCode"
        val expected = ProposedMeetingDTO(
            proposedSlots = listOf(MeetingSlotDTO(_10am, _11am)),
            initiator = InitiatorDTO(
                initiatorEntity.fullName,
                initiatorEntity.email
            ),
            appLinks = MeetingAppLinksDTO(
                iosAppDownloadLink = "https://meetsamatest.page.link/?link=$expectedUrl&param1=value1&param2=value2"
            )
        )

        assertEquals(expected, actual)
    }
}