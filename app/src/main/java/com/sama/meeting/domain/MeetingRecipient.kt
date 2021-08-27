package com.sama.meeting.domain

import com.sama.common.Factory
import com.sama.users.application.UserInternalDTO
import com.sama.users.domain.InvalidEmailException
import com.sama.users.domain.UserId
import org.apache.commons.validator.routines.EmailValidator

data class MeetingRecipient(val recipientId: UserId?, val email: String?) {
    @Factory
    companion object {
        fun fromUser(user: UserInternalDTO): MeetingRecipient {
            return MeetingRecipient(user.id, user.email)
        }

        fun fromUser(userId: UserId, email: String): MeetingRecipient {
            return MeetingRecipient(userId, email)
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