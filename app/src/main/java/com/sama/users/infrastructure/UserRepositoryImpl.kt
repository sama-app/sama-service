package com.sama.users.infrastructure

import com.sama.common.findByIdOrThrow
import com.sama.users.domain.UserDetails
import com.sama.users.domain.UserDeviceRegistrations
import com.sama.users.domain.UserGoogleCredential
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import com.sama.users.domain.UserRepository
import com.sama.users.infrastructure.jpa.UserEntity
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.sama.users.infrastructure.jpa.applyChanges
import com.sama.users.infrastructure.jpa.findByEmailOrThrow
import com.sama.users.infrastructure.jpa.findByPublicIdOrThrow
import com.sama.users.infrastructure.jpa.findIdByEmailOrThrow
import com.sama.users.infrastructure.jpa.findIdByPublicIdOrThrow
import java.util.UUID
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
    private val googleTokenEncryptor: TextEncryptor
) : UserRepository {
    override fun findByIdOrThrow(userId: UserId): UserDetails {
        return userJpaRepository.findByIdOrThrow(userId.id).toUserDetails()
    }

    override fun findByIds(ids: Collection<UserId>): List<UserDetails> {
        return userJpaRepository.findAllById(ids.map { it.id })
            .map { it.toUserDetails() }
    }

    override fun findDeviceRegistrationsByIdOrThrow(userId: UserId): UserDeviceRegistrations {
        return userJpaRepository.findByIdOrThrow(userId.id).toDeviceRegistrations()
    }

    override fun findByEmailOrThrow(email: String): UserDetails {
        return userJpaRepository.findByEmailOrThrow(email).toUserDetails()
    }

    override fun findByPublicIdOrThrow(publicId: UserPublicId): UserDetails {
        return userJpaRepository.findByPublicIdOrThrow(publicId.id).toUserDetails()
    }

    override fun existsByEmail(email: String): Boolean {
        return userJpaRepository.existsByEmail(email)
    }

    override fun findIdByEmailOrThrow(email: String): UserId {
        return userJpaRepository.findIdByEmailOrThrow(email).toUserId()
    }

    override fun findIdByPublicIdOrThrow(userPublicId: UserPublicId): UserId {
        return userJpaRepository.findIdByPublicIdOrThrow(userPublicId.id).toUserId()
    }

    override fun save(userDetails: UserDetails): UserDetails {
        return if (userDetails.id == null) {
            var userEntity = UserEntity.new(userDetails)
            userEntity = userJpaRepository.save(userEntity)
            userEntity.toUserDetails()
        } else {
            var userEntity = userJpaRepository.findByIdOrThrow(userDetails.id.id)
            userEntity= userEntity.applyChanges(userDetails)
            userEntity = userJpaRepository.save(userEntity)
            userEntity.toUserDetails()
        }
    }

    override fun save(userGoogleCredential: UserGoogleCredential): UserGoogleCredential {
        var userEntity = userJpaRepository.findByIdOrThrow(userGoogleCredential.userId.id)
        userEntity.applyChanges(userGoogleCredential.googleCredential.encrypt(googleTokenEncryptor))
        userEntity = userJpaRepository.save(userEntity)
        return userGoogleCredential.copy(googleCredential = userEntity.googleCredential!!)
    }

    override fun save(userDeviceRegistrations: UserDeviceRegistrations): UserDeviceRegistrations {
        var userEntity = userJpaRepository.findByIdOrThrow(userDeviceRegistrations.userId.id)
        userEntity = userEntity.applyChanges(userDeviceRegistrations)
        userEntity = userJpaRepository.save(userEntity)
        return userEntity.toDeviceRegistrations()
    }

    override fun deleteAll() {
        userJpaRepository.deleteAll()
    }
}

fun UserEntity.toUserDetails(): UserDetails {
    return UserDetails(
        this.id?.toUserId(),
        this.publicId?.toUserPublicId(),
        this.email,
        this.fullName,
        this.active!!
    )
}

fun UserEntity.toDeviceRegistrations(): UserDeviceRegistrations {
    return UserDeviceRegistrations(
        this.id!!.toUserId(),
        this.firebaseCredential?.deviceId,
        this.firebaseCredential?.registrationToken
    )
}

fun Long.toUserId(): UserId {
    return UserId(this)
}

fun UUID.toUserPublicId(): UserPublicId {
    return UserPublicId(this)
}