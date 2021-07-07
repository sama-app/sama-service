package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingProposalMessageConfiguration
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.MeetingInvitation
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.*
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
        MeetingInvitationService::class,
        MeetingProposalMessageConfiguration::class,
        MeetingInvitationServiceTestConfiguration::class
    ]
)
class MeetingInvitationServiceTest {
    @Autowired
    lateinit var underTest: MeetingInvitationService

    @Test
    fun `find for proposed meeting`() {
        val targetZoneId = ZoneId.of("Europe/Rome")
        val _9am = ZonedDateTime.of(LocalDate.now(), LocalTime.of(9, 0), ZoneId.of("UTC"))
        val _10am = _9am.plusHours(1)
        val _11am = _9am.plusHours(2)

        val meetingInvitation = underTest.findForProposedMeeting(
            ProposedMeeting(
                21L, 11L, 1L,
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
            * Jul 7 11:00 AM - 11:15 AM (CET)
            * Jul 7 12:00 PM - 1:00 PM (CET)

            Pick here $expectedUrl
        """.trimIndent()

        assertEquals(expectedUrl, meetingInvitation.url)
        assertEquals(expectedMessage, meetingInvitation.message)
    }
}