package com.sama.meeting.domain

import com.sama.common.DomainValidationException
import java.time.Duration

class InvalidDurationException(meetingIntentId: MeetingIntentId, duration: Duration) :
    DomainValidationException("MeetingIntent#$meetingIntentId: Unsupported duration '${duration.toMinutes()} min'")

class InvalidMeetingSlotException(meetingIntentId: MeetingIntentId, slot: MeetingSlot) :
    DomainValidationException("MeetingIntent#$meetingIntentId: Invalid slot '${slot.startTime} - ${slot.endTime}'")

class InvalidMeetingProposalException(meetingIntentId: MeetingIntentId, message: String) :
    DomainValidationException("MeetingIntent#$meetingIntentId cannot be proposed: $message")

class MeetingAlreadyConfirmedException(meetingId: MeetingId) :
    DomainValidationException("Meeting#$meetingId already confirmed")

class MeetingProposalExpiredException(meetingId: MeetingId) :
    DomainValidationException("Meeting#$meetingId has expired")

class InvalidMeetingStatusException(meetingId: MeetingId, status: MeetingStatus):
    DomainValidationException("Meeting#$meetingId: Invalid status: $status")

class MeetingSlotUnavailableException(meetingId: MeetingId, slot: MeetingSlot):
    DomainValidationException("Meeting#$meetingId: Slot unavailable: '${slot.startTime} - ${slot.endTime}'")
