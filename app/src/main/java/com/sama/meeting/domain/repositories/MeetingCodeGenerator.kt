package com.sama.meeting.domain.repositories

import com.sama.meeting.domain.MeetingCode

interface MeetingCodeGenerator {
    fun generate(): MeetingCode
}