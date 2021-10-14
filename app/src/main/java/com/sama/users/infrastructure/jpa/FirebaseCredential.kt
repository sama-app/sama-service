package com.sama.users.infrastructure.jpa

import java.time.Instant
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(schema = "sama", name = "user_firebase_credential")
class FirebaseCredential(
    @Id
    var deviceId: UUID,

    @Column
    private val userId: Long,

    @Column(name = "firebase_registration_token")
    var registrationToken: String,

    @Column
    var updatedAt: Instant,
) {

    fun update(registrationToken: String): FirebaseCredential {
        this.registrationToken = registrationToken
        this.updatedAt = Instant.now()
        return this
    }
}
