package com.sama.users.infrastructure

import com.sama.common.findByIdOrThrow
import com.sama.users.domain.*
import com.sama.users.infrastructure.jpa.*
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(private val userJpaRepository: UserJpaRepository) : UserRepository {
    override fun findByIdOrThrow(userId: UserId): UserDetails {
        return userJpaRepository.findByIdOrThrow(userId).toUserDetails()
    }

    override fun findJwtIssuerByIdOrThrow(userId: UserId): UserJwtIssuer {
        return userJpaRepository.findByIdOrThrow(userId).toJwtIssuer()
    }

    override fun findJwtIssuerByEmailOrThrow(email: String): UserJwtIssuer {
        return userJpaRepository.findByEmailOrThrow(email).toJwtIssuer()
    }

    override fun findJwtIssuerByPublicOrThrow(publicId: UserPublicId): UserJwtIssuer {
        return userJpaRepository.findByPublicIdOrThrow(publicId).toJwtIssuer()
    }

    override fun findDeviceRegistrationsByIdOrThrow(userId: UserId): UserDeviceRegistrations {
        return userJpaRepository.findByIdOrThrow(userId).toDeviceRegistrations()
    }

    override fun findAllIds(): Set<UserId> {
        return userJpaRepository.findAllIds()
    }

    override fun findPublicDetailsById(ids: Set<UserId>): List<UserDetails> {
        return userJpaRepository.findPublicDetailsById(ids)
    }

    override fun findByEmailOrThrow(email: String): UserDetails {
        return userJpaRepository.findByEmailOrThrow(email).toUserDetails()
    }

    override fun findByPublicIdOrThrow(publicId: UserPublicId): UserDetails {
        return userJpaRepository.findByPublicIdOrThrow(publicId).toUserDetails()
    }

    override fun existsByEmail(email: String): Boolean {
        return userJpaRepository.existsByEmail(email)
    }

    override fun findIdByEmailOrThrow(email: String): UserId {
        return userJpaRepository.findIdByEmailOrThrow(email)
    }

    override fun findIdByPublicIdOrThrow(userPublicId: UserPublicId): UserId {
        return userJpaRepository.findIdByPublicIdOrThrow(userPublicId)
    }

    override fun save(userDetails: UserDetails): UserDetails {
        var userEntity = UserEntity.new(userDetails)
        userEntity = userJpaRepository.save(userEntity)
        return userEntity.toUserDetails()
    }

    override fun save(userGoogleCredential: UserGoogleCredential): UserGoogleCredential {
        var userEntity = userJpaRepository.findByIdOrThrow(userGoogleCredential.userId)
        userEntity.applyChanges(userGoogleCredential.googleCredential)
        userEntity = userJpaRepository.save(userEntity)
        return userGoogleCredential.copy(googleCredential = userEntity.googleCredential!!)
    }

    override fun save(userDeviceRegistrations: UserDeviceRegistrations): UserDeviceRegistrations {
        var userEntity = userJpaRepository.findByIdOrThrow(userDeviceRegistrations.userId)
        userEntity = userEntity.applyChanges(userDeviceRegistrations)
        userEntity = userJpaRepository.save(userEntity)
        return userEntity.toDeviceRegistrations()
    }
}

fun UserEntity.toUserDetails(): UserDetails {
    return UserDetails(
        this.id,
        this.publicId,
        this.email,
        this.fullName
    )
}

fun UserEntity.toJwtIssuer(): UserJwtIssuer {
    return UserJwtIssuer(
        this.id!!,
        this.publicId!!,
        this.email,
        this.active!!
    )
}

fun UserEntity.toDeviceRegistrations(): UserDeviceRegistrations {
    return UserDeviceRegistrations(
        this.id!!,
        this.firebaseCredential?.deviceId,
        this.firebaseCredential?.registrationToken
    )
}