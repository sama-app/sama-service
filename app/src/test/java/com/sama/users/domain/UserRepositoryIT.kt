package com.sama.users.domain

import com.sama.common.BasePersistenceIT
import com.sama.users.infrastructure.UserRepositoryImpl
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [UserRepositoryImpl::class])
class UserRepositoryIT : BasePersistenceIT<UserRepository>() {

    @MockBean
    private lateinit var textEncryptor: TextEncryptor

    @Test
    fun `save user details`() {
        val email = "test@meetsama.com"
        val fullName = "Test"
        val persisted = underTest.save(UserDetails(email = email, fullName = fullName, active = true))

        assertThat(persisted.id).isNotNull
        assertThat(persisted.publicId).isNotNull
        assertThat(persisted.email).isEqualTo(email)
        assertThat(persisted.fullName).isEqualTo(fullName)
        assertThat(persisted.active).isTrue
    }

    @Test
    fun `save device registrations`() {
        val userId = underTest.save(UserDetails(email = "test@meetsama.com", fullName = "Test", active = true)).id!!

        val toPersist = UserDeviceRegistrations(
            userId, setOf(
                DeviceRegistration(UUID.randomUUID(), "some-token"),
                DeviceRegistration(UUID.randomUUID(), "some-token2")
            )
        )
        underTest.save(toPersist)

        var actual = underTest.findDeviceRegistrationsByIdOrThrow(userId)
        assertThat(actual).isEqualTo(toPersist)

        val updated = UserDeviceRegistrations(
            userId,
            setOf(DeviceRegistration(UUID.randomUUID(), "some-token3"))
        )
        underTest.save(updated)

        actual = underTest.findDeviceRegistrationsByIdOrThrow(userId)
        assertThat(actual).isEqualTo(updated)
    }

    @Test
    fun `test query methods`() {
        val email = "test@meetsama.com"
        val expected = underTest.save(UserDetails(email = email, fullName = "some name", active = true))
        val userId = expected.id!!
        val publicId = expected.publicId!!

        assertThat(underTest.findByIdOrThrow(userId)).isEqualTo(expected)
        assertThat(underTest.findByEmailOrThrow(email)).isEqualTo(expected)
        assertThat(underTest.findByPublicIdOrThrow(publicId)).isEqualTo(expected)

        assertThat(underTest.findByIds(setOf(userId, UserId(1000)))).isEqualTo(listOf(expected))
        assertThat(underTest.findByIds(setOf())).isEmpty()

        assertThat(underTest.findIdByEmailOrThrow(email)).isEqualTo(userId)
        assertThat(underTest.findIdByPublicIdOrThrow(publicId)).isEqualTo(userId)

        assertThat(underTest.existsByEmail(email)).isTrue()
        assertThat(underTest.existsByEmail("fake@meetsama.com")).isFalse()
    }
}