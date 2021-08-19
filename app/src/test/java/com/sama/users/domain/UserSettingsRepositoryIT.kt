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
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [UserRepositoryImpl::class, UserSettingsRepositoryImpl::class])
class UserSettingsRepositoryIT : BasePersistenceIT<UserSettingsRepository>() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun save() {
        val userId = UserId(1)
        userRepository.save(UserDetails(id = userId, email = "test@meetsama.com", fullName = "test", active = true))

        val toPersist = UserSettings(
            userId, Locale.ENGLISH, ZoneId.of("UTC"), true,
            mapOf(
                DayOfWeek.TUESDAY to WorkingHours.nineToFive(),
                DayOfWeek.FRIDAY to WorkingHours.nineToFive()
            )
        )
        underTest.save(toPersist)

        val actual = underTest.findByIdOrThrow(userId)
        assertThat(actual).isEqualTo(toPersist)
    }

}