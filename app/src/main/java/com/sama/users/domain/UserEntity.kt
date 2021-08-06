package com.sama.users.domain

import com.sama.common.AggregateRoot
import com.sama.common.Factory
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.SecondaryTable
import javax.persistence.SecondaryTables
import javax.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime
import org.hibernate.annotations.GenerationTime.INSERT
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

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
        fun new(userRegistration: UserRegistration): UserEntity {
            val user = UserEntity(userRegistration.email)
            user.fullName = userRegistration.fullName
            user.googleCredential = userRegistration.credential.copy(updatedAt = Instant.now())
            return user
        }
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: UserId? = null

    @Column(nullable = false)
    @Generated(value = INSERT)
    var publicId: UserPublicId? = null

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

fun UserEntity.applyChanges(googleCredential: GoogleCredential): UserEntity {
    this.googleCredential = this.googleCredential?.merge(googleCredential)
        ?: googleCredential.copy(updatedAt = Instant.now())
    return this
}

fun UserEntity.applyChanges(userDetails: UserPublicDetails): UserEntity {
    this.fullName = userDetails.fullName
    return this
}