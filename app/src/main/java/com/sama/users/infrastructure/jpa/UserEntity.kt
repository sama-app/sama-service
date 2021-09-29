package com.sama.users.infrastructure.jpa

import com.sama.common.AggregateRoot
import com.sama.common.Factory
import com.sama.users.domain.UserDetails
import com.sama.users.domain.UserDeviceRegistrations
import java.time.Instant
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime.INSERT
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

@AggregateRoot
@Entity
@Table(schema = "sama", name = "user")
class UserEntity(email: String) {

    @Factory
    companion object {
        fun new(userDetails: UserDetails): UserEntity {
            val user = UserEntity(userDetails.email)
            user.id = userDetails.id?.id
            user.publicId = userDetails.publicId?.id
            user.fullName = userDetails.fullName
            return user
        }
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    @Generated(value = INSERT)
    var publicId: UUID? = null

    @Column(nullable = false)
    var email: String = email

    @Column
    var fullName: String? = null

    @Column(nullable = false)
    var active: Boolean? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, updatable = false, insertable = false)
    var firebaseCredentials: MutableList<FirebaseCredential> = mutableListOf()

    @CreatedDate
    @Column(nullable = false)
    var createdAt: Instant

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant

    init {
        this.active = true
        this.createdAt = Instant.now()
        this.updatedAt = Instant.now()
    }
}

fun UserEntity.applyChanges(user: UserDeviceRegistrations): UserEntity {
    val existing = firebaseCredentials.associateBy { it.deviceId }
    this.firebaseCredentials.clear()
    this.firebaseCredentials.addAll(
        user.deviceRegistrations.mapTo(mutableListOf())
        { (deviceId, firebaseRegistrationToken) ->
            existing[deviceId]
                ?.copy(registrationToken = firebaseRegistrationToken, updatedAt = Instant.now())
                ?: FirebaseCredential(
                    deviceId,
                    user.userId.id,
                    firebaseRegistrationToken,
                    Instant.now()
                )
        })
    return this
}

fun UserEntity.applyChanges(userDetails: UserDetails): UserEntity {
    this.fullName = userDetails.fullName
    return this
}