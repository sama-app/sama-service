package com.sama.meeting.domain

import com.sama.meeting.domain.MeetingCode

interface MeetingCodeGenerator {
    fun generate(): MeetingCode
}