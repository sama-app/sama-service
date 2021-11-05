package com.sama.users.infrastructure

import com.sama.common.BasePersistenceIT
import com.sama.users.domain.MeetingPreferences
import com.sama.users.domain.UserDetails
import com.sama.users.domain.UserPermission
import com.sama.users.domain.UserRepository
import com.sama.users.domain.UserSettings
import com.sama.users.domain.UserSettingsRepository
import com.sama.users.domain.WorkingHours
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [UserRepositoryImpl::class, UserSettingsRepositoryImpl::class])
class UserSettingsRepositoryIT : BasePersistenceIT<UserSettingsRepository>() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun save() {
        val user = userRepository.save(UserDetails(email = "test@meetsama.com", fullName = "test", active = true))

        val toPersist = UserSettings(
            user.id!!, Locale.ENGLISH, ZoneId.of("UTC"), true,
            mapOf(
                DayOfWeek.TUESDAY to WorkingHours.nineToFive(),
                DayOfWeek.FRIDAY to WorkingHours.nineToFive()
            ),
            MeetingPreferences.default(),
            true,
            emptySet()
        )
        underTest.save(toPersist)

        var actual = underTest.findByIdOrThrow(user.id!!)
        assertThat(actual).isEqualTo(toPersist)

        val toUpdate = UserSettings(
            user.id!!, Locale.ITALIAN, ZoneId.of("Europe/Rome"), false,
            mapOf(
                DayOfWeek.WEDNESDAY to WorkingHours.nineToFive(),
            ),
            MeetingPreferences.default(),
            false,
            setOf(UserPermission.PAST_EVENT_CONTACT_SCAN)
        )
        underTest.save(toUpdate)

        actual = underTest.findByIdOrThrow(user.id!!)
        assertThat(actual).isEqualTo(toUpdate)
    }

}