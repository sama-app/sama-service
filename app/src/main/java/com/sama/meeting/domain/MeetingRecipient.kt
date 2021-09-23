package com.sama.meeting.domain

import com.sama.users.domain.InvalidEmailException
import com.sama.users.domain.UserId
import org.apache.commons.validator.routines.EmailValidator

sealed interface MeetingRecipient

data class UserRecipient private constructor(val recipientId: UserId) : MeetingRecipient {

    companion object {
        fun of(userId: UserId): UserRecipient {
            return UserRecipient(userId)
        }
    }
}

data class EmailRecipient private constructor(val email: String) : MeetingRecipient {

    companion object {
        fun of(email: String): EmailRecipient {
            val isEmailValid = EmailValidator.getInstance().isValid(email)
            if (!isEmailValid) {
                throw InvalidEmailException(email)
            }
            return EmailRecipient(email)
        }
    }
}