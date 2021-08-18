package com.sama.meeting.domain

import com.sama.common.Factory
import com.sama.common.ValueObject
import com.sama.users.application.UserInternalDTO
import com.sama.users.application.UserPublicDTO
import com.sama.users.domain.InvalidEmailException
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.jpa.UserEntity
import org.apache.commons.validator.routines.EmailValidator
import javax.persistence.Embeddable

@Embeddable
@ValueObject
data class MeetingRecipient(val recipientId: UserId?, val email: String?) {
    @Factory
    companion object {
        fun fromUser(user: UserInternalDTO): MeetingRecipient {
            return MeetingRecipient(user.id, user.email)
        }

        fun fromUserId(userId: UserId): MeetingRecipient {
            return MeetingRecipient(userId, null)
        }

        fun fromEmail(email: String): MeetingRecipient {
            val isEmailValid = EmailValidator.getInstance().isValid(email)
            if (!isEmailValid) {
                throw InvalidEmailException(email)
            }
            return MeetingRecipient(null, email)
        }
    }
}