package com.sama.comms.domain

import com.sama.users.domain.UserId
import java.time.ZoneId

data class CommsUser(
    val userId: UserId,
    val timeZone: ZoneId,
    val email: String
)