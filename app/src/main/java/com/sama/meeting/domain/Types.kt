package com.sama.meeting.domain

import java.util.UUID

@JvmInline
value class MeetingIntentId(val id: Long)

@JvmInline
value class MeetingIntentCode(val code: UUID) {
    companion object {
        fun random() = MeetingIntentCode(UUID.randomUUID())
    }
}

@JvmInline
value class MeetingId(val id: Long)

@JvmInline
value class MeetingCode(val code: String)