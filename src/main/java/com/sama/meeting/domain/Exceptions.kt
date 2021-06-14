package com.sama.meeting.domain

import com.sama.common.DomainValidationException
import java.time.Duration

class InvalidDurationException(meetingIntentId: MeetingIntentId, duration: Duration) :
    DomainValidationException("Unsupported duration '${duration.toMinutes()} min' for Meeting#$meetingIntentId")

class InvalidMeetingSlotException(meetingIntentId: MeetingIntentId, slot: MeetingSlot) :
    DomainValidationException("Invalid slot '${slot.startTime} - ${slot.endTime}' for Meeting#$meetingIntentId")

class InvalidMeetingProposalException(meetingIntentId: MeetingIntentId, message: String) :
    DomainValidationException("Cannot propose Meeting#$meetingIntentId: $message")