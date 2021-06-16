package com.sama.meeting.domain

import com.sama.common.Factory
import com.sama.common.ValueObject
import com.sama.users.domain.UserId
import javax.persistence.Embeddable

@Embeddable
@ValueObject
data class MeetingRecipient(val recipientId: UserId?, val email: String?) {
    @Factory
    companion object {
        fun fromEmail(email: String): MeetingRecipient {
            return MeetingRecipient(null, email)
        }
    }
}