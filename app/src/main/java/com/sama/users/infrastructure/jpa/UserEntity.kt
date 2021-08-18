package com.sama.users.infrastructure.jpa

import com.sama.common.AggregateRoot
import com.sama.common.Factory
import com.sama.users.domain.*
import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime.INSERT
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.util.UUID
import javax.persistence.*
import javax.persistence.GenerationType.IDENTITY

@AggregateRoot
@Entity
@Table(schema = "sama", name = "user")
@SecondaryTables(
    value = [
        SecondaryTable(
            schema = "sama", name = "user_google_credential",
            pkJoinColumns = [PrimaryKeyJoinColumn(name = "user_id")]
        ),
        SecondaryTable(
            schema = "sama", name = "user_firebase_credential",
            pkJoinColumns = [PrimaryKeyJoinColumn(name = "user_id")]
        )
    ]
)
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

    @Embedded
    var googleCredential: GoogleCredential? = null

    @Embedded
    var firebaseCredential: FirebaseCredential? = null

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
    this.firebaseCredential = if (user.deviceId != null && user.firebaseRegistrationToken != null) {
        FirebaseCredential(user.deviceId, user.firebaseRegistrationToken, Instant.now())
    } else {
        null
    }
    return this
}

fun UserEntity.applyChanges(userGoogleCredential: UserGoogleCredential): UserEntity {
    val newGoogleCredential = userGoogleCredential.googleCredential
    this.googleCredential = this.googleCredential?.merge(newGoogleCredential)
        ?: newGoogleCredential.copy(updatedAt = Instant.now())
    return this
}

fun UserEntity.applyChanges(googleCredential: GoogleCredential): UserEntity {
    this.googleCredential = this.googleCredential?.merge(googleCredential)
        ?: googleCredential.copy(updatedAt = Instant.now())
    return this
}

fun UserEntity.applyChanges(userDetails: UserDetails): UserEntity {
    this.fullName = userDetails.fullName
    return this
}