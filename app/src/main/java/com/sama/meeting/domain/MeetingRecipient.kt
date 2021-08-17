package com.sama.meeting.domain

import com.sama.common.Factory
import com.sama.common.ValueObject
import com.sama.users.domain.InvalidEmailException
import com.sama.users.domain.UserEntity
import com.sama.users.domain.UserId
import org.apache.commons.validator.routines.EmailValidator
import javax.persistence.Embeddable

@Embeddable
@ValueObject
data class MeetingRecipient(val recipientId: UserId?, val email: String?) {
    @Factory
    companion object {
        fun fromUser(userEntity: UserEntity): MeetingRecipient {
            return MeetingRecipient(userEntity.id!!, userEntity.email)
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