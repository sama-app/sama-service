package com.sama.users.infrastructure.jpa

import com.sama.common.ValueObject
import java.time.Instant
import java.util.*
import javax.persistence.*

@ValueObject
@Embeddable
data class FirebaseCredential(

    @Column(name = "device_id", table = "user_firebase_credential")
    val deviceId: UUID,

    @Column(name = "firebase_registration_token", table = "user_firebase_credential")
    val registrationToken: String,

    @Column(name = "updated_at", table = "user_firebase_credential")
    val updatedAt: Instant
)
