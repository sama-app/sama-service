package com.sama.users.domain

import com.sama.common.BasePersistenceIT
import com.sama.users.infrastructure.UserRepositoryImpl
import com.sama.users.infrastructure.UserSettingsRepositoryImpl
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.encrypt.TextEncryptor
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
            false,
            setOf(UserPermission.PAST_EVENT_CONTACT_SCAN)
        )
        underTest.save(toUpdate)

        actual = underTest.findByIdOrThrow(user.id!!)
        assertThat(actual).isEqualTo(toUpdate)
    }

}