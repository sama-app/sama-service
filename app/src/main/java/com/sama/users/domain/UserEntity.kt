package com.sama.users.domain

import com.sama.common.AggregateRoot
import com.sama.common.Factory
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import javax.persistence.*

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
            user.id = userRegistration.userId
            user.fullName = userRegistration.fullName
            user.googleCredential = userRegistration.credential.copy(updatedAt = Instant.now())
            return user
        }
    }

    @Id
    private var id: UserId? = null

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

    fun id(): Long? {
        return id
    }

    fun email(): String {
        return email
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

fun UserEntity.applyChanges(userDetails: BasicUserDetails): UserEntity {
    this.fullName = userDetails.fullName
    return this
}