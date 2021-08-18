package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingProposalMessageConfiguration
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.infrastructure.jpa.UserEntity
import com.sama.users.infrastructure.jpa.UserJpaRepository
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
class MeetingInvitationServiceTestConfiguration {
    @Bean
    fun meetingUrlConfiguration(): MeetingUrlConfiguration {
        return MeetingUrlConfiguration(10, scheme, host)
    }
}

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        MeetingInvitationView::class,
        MeetingProposalMessageConfiguration::class,
        MeetingInvitationServiceTestConfiguration::class
    ]
)
class MeetingInvitationViewTest {
    @MockBean
    lateinit var userRepository: UserJpaRepository

    @Autowired
    lateinit var underTest: MeetingInvitationView

    @Test
    fun render() {
        val targetZoneId = ZoneId.of("Europe/Rome")
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

        val actual = underTest.render(
            ProposedMeeting(
                21L, 11L, initiatorId,
                Duration.ofMinutes(15),
                listOf(
                    MeetingSlot(_9am, _9am.plusMinutes(15)),
                    MeetingSlot(_10am, _11am)
                ),
                meetingCode
            ),
            targetZoneId
        )

        // verify
        val expectedUrl = "$scheme://$host/$meetingCode"
        val expectedMessage = """
            * Jul 7 11:00 AM - 11:15 AM (GMT+2)
            * Jul 7 12:00 PM - 1:00 PM (GMT+2)

            Pick here $expectedUrl
        """.trimIndent()
        val expected = MeetingInvitationDTO(
            meeting = MeetingDTO(
                initiator = InitiatorDTO(
                    initiatorEntity.fullName,
                    initiatorEntity.email
                ),
                proposedSlots = listOf(
                    MeetingSlotDTO(_9am, _9am.plusMinutes(15)),
                    MeetingSlotDTO(_10am, _11am)
                )
            ),
            meetingCode = meetingCode,
            shareableMessage = expectedMessage,
            meetingUrl = expectedUrl
        )

        assertEquals(expected, actual)
    }
}