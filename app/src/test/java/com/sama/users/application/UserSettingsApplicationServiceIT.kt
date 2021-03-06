package com.sama.users.application

import com.sama.common.BaseApplicationIntegrationTest
import com.sama.users.domain.UserSettingsDefaults
import com.sama.users.domain.UserSettingsDefaultsRepository
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean


class UserSettingsApplicationServiceIT : BaseApplicationIntegrationTest() {

    @MockBean
    lateinit var userSettingsDefaultsRepository: UserSettingsDefaultsRepository

    @Autowired
    lateinit var underTest: UserSettingsApplicationService

    @BeforeEach
    fun setupUserSettingsDefaults() {
        val userId = initiator().id
        whenever(userSettingsDefaultsRepository.findById(userId))
            .thenReturn(
                UserSettingsDefaults(
                    Locale.ENGLISH,
                    ZoneId.of("Europe/Rome"),
                    true
                )
            )
    }

    @Test
    fun `create user settings from defaults`() {
        val userId = initiator().id
        underTest.create(userId)

        val actual = underTest.find(userId)

        val expected = UserSettingsDTO(
            Locale.ENGLISH,
            ZoneId.of("Europe/Rome"),
            true,
            listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
                .map { DayWorkingHoursDTO(it, LocalTime.of(9, 0), LocalTime.of(17, 0)) },
            emptySet(),
            MarketingPreferencesDTO(false),
            MeetingPreferencesDTO(null, true)
        )


        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `update user settings`() {
        val userId = initiator().id
        underTest.create(userId)

        val newTimeZone = ZoneId.of("Europe/Vilnius")
        asInitiator {
            underTest.updateTimeZone(UpdateTimeZoneCommand(newTimeZone))
            assertThat(underTest.me().timeZone).isEqualTo(newTimeZone)
        }

        val newWorkingHours = listOf(DayWorkingHoursDTO(MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)))
        asInitiator {
            underTest.updateWorkingHours(UpdateWorkingHoursCommand(newWorkingHours))
            assertThat(underTest.me().workingHours).isEqualTo(newWorkingHours)
        }

        asInitiator {
            underTest.updateMeetingPreferences(UpdateMeetingPreferencesCommand("Test", false))
            assertThat(underTest.me().meetingPreferences)
                .isEqualTo(MeetingPreferencesDTO("Test", false))
        }
    }
}