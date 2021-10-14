package com.sama.meeting.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.sama.meeting.configuration.MeetingProposalMessageConfiguration
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingId
import com.sama.meeting.domain.MeetingIntentId
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.SamaNonSamaProposedMeeting
import com.sama.users.application.MarketingPreferencesDTO
import com.sama.users.application.UserPublicDTO
import com.sama.users.application.UserService
import com.sama.users.application.UserSettingsDTO
import com.sama.users.application.UserSettingsService
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension

private const val scheme = "https"
private const val host = "app.meetsama.com"
private val meetingCode = MeetingCode("VGsUTGno")

@TestConfiguration
class MeetingInvitationServiceTestConfiguration {
    @Bean
    fun meetingUrlConfiguration() = MeetingUrlConfiguration(10, scheme, host)

    @Bean
    fun objectMapper() = ObjectMapper().findAndRegisterModules()
}

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        MeetingInvitationView::class,
        MeetingProposalMessageConfiguration::class,
        MeetingInvitationServiceTestConfiguration::class,
    ]
)
class MeetingInvitationViewTest {
    private val userId = UserId(1)
    private val _9am = ZonedDateTime.of(
        LocalDate.of(2021, 7, 7),
        LocalTime.of(9, 0), ZoneOffset.UTC
    )
    private val _10am = _9am.plusHours(1)
    private val _11am = _9am.plusHours(2)
    private val slots = listOf(
        MeetingSlot(_10am, _11am),
        MeetingSlot(_9am, _9am.plusMinutes(15)),
        MeetingSlot(_10am.plusDays(1), _11am.plusDays(1)),
    )
    private val meetingUrl = "$scheme://$host/${meetingCode.code}"

    @MockBean
    lateinit var userService: UserService

    @MockBean
    lateinit var userSettingsService: UserSettingsService

    @Autowired
    lateinit var underTest: MeetingInvitationView

    @Test
    fun render() {
        val meetingTitle = "Meeting with test"
        val initiator = UserPublicDTO(UserPublicId.random(), "test", "test@meetsama.com")
        val recipientZoneId = ZoneOffset.UTC
        val initiatorZoneId = ZoneOffset.UTC
        val settings = UserSettingsDTO(
            Locale.forLanguageTag("en"), initiatorZoneId, true,
            emptyList(), emptySet(), MarketingPreferencesDTO(true)
        )
        whenever(userService.find(userId)).thenReturn(initiator)
        whenever(userSettingsService.find(userId)).thenReturn(settings)

        val actual = underTest.render(
            SamaNonSamaProposedMeeting(
                MeetingId(21), MeetingIntentId(11L), Duration.ofMinutes(15),
                userId,
                slots,
                meetingCode,
                meetingTitle,
                ZonedDateTime.now()
            ),
            recipientZoneId
        )

        // verify
        val expectedMessage = """
            Would any of these work for you?
            
            Wed, July 7:
            * 9:00 AM - 9:15 AM
            * 10:00 AM - 11:00 AM
            Thu, July 8:
            * 10:00 AM - 11:00 AM
            
            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent()
        val expected = MeetingInvitationDTO(
            meeting = MeetingDTO(
                initiator = UserPublicDTO(
                    initiator.userId,
                    initiator.fullName,
                    initiator.email
                ),
                proposedSlots = listOf(
                    MeetingSlotDTO(_9am, _9am.plusMinutes(15)),
                    MeetingSlotDTO(_10am, _11am),
                    MeetingSlotDTO(_10am.plusDays(1), _11am.plusDays(1))
                ),
                title = meetingTitle,
            ),
            meetingCode = meetingCode,
            shareableMessage = expectedMessage,
            meetingUrl = meetingUrl
        )

        assertEquals(expected, actual)
    }


    data class TestInput(val initiatorTimeZone: ZoneId, val initiatorLocale: Locale, val recipientTimeZone: ZoneId)

    @TestFactory
    fun renderShareableMessage() = listOf(
        TestInput(ZoneId.of("Europe/London"), Locale.UK, ZoneId.of("Europe/London")) to """
            Would any of these work for you?
            
            Wed, July 7:
            * 10:00 - 10:15
            * 11:00 - 12:00
            Thu, July 8:
            * 11:00 - 12:00

            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent(),

        TestInput(ZoneId.of("America/New_York"), Locale.US, ZoneId.of("Europe/London")) to """
            Would any of these work for you? Times are in GMT.
            
            Wed, July 7:
            * 10:00 - 10:15
            * 11:00 - 12:00
            Thu, July 8:
            * 11:00 - 12:00
            
            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent(),

        TestInput(ZoneId.of("Europe/London"), Locale.UK, ZoneId.of("Europe/Vilnius")) to """
            Would any of these work for you? Times are in EET.
            
            Wed, July 7:
            * 12:00 - 12:15
            * 13:00 - 14:00
            Thu, July 8:
            * 13:00 - 14:00

            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent(),

        TestInput(ZoneId.of("Europe/Vilnius"), Locale.forLanguageTag("LT"), ZoneId.of("Europe/Vilnius")) to """
            Would any of these work for you?
            
            Wed, July 7:
            * 12:00 - 12:15
            * 13:00 - 14:00
            Thu, July 8:
            * 13:00 - 14:00

            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent(),

        TestInput(ZoneId.of("Europe/London"), Locale.UK, ZoneId.of("America/New_York")) to """
            Would any of these work for you? Times are in EST.
            
            Wed, July 7:
            * 5:00 AM - 5:15 AM
            * 6:00 AM - 7:00 AM
            Thu, July 8:
            * 6:00 AM - 7:00 AM

            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent(),

        TestInput(ZoneId.of("America/New_York"), Locale.US, ZoneId.of("America/New_York")) to """
            Would any of these work for you?
            
            Wed, July 7:
            * 5:00 AM - 5:15 AM
            * 6:00 AM - 7:00 AM
            Thu, July 8:
            * 6:00 AM - 7:00 AM

            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent(),

        TestInput(ZoneId.of("America/New_York"), Locale.US, ZoneId.of("America/New_York")) to """
            Would any of these work for you?
            
            Wed, July 7:
            * 5:00 AM - 5:15 AM
            * 6:00 AM - 7:00 AM
            Thu, July 8:
            * 6:00 AM - 7:00 AM

            You can pick a suitable time here: https://app.meetsama.com/VGsUTGno
        """.trimIndent(),
    )
        .map { (input, expected) ->
            dynamicTest("$input renders") {
                val actual = underTest.renderShareableMessage(
                    slots, meetingUrl,
                    input.initiatorTimeZone,
                    input.initiatorLocale,
                    input.recipientTimeZone
                )
                assertThat(actual).isEqualTo(expected)
            }
        }
}