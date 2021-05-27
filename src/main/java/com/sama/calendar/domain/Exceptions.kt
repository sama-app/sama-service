package com.sama.calendar.domain

import com.sama.common.DomainValidationException
import java.time.Duration


class InvalidDurationException(meetingId: MeetingId, duration: Duration) :
    DomainValidationException("Unsupported duration '${duration.toMinutes()} min' for Meeting#$meetingId")

class InvalidMeetingSlotException(meetingId: MeetingId, slot: MeetingSlot) :
    DomainValidationException("Invalid slot '${slot.startTime} - ${slot.endTime}' for Meeting#$meetingId")

class InvalidMeetingProposalException(meetingId: MeetingId, message: String) :
    DomainValidationException("Cannot propose Meeting#$meetingId: $message")