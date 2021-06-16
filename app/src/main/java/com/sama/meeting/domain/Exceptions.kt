package com.sama.meeting.domain

import com.sama.common.DomainValidationException
import java.time.Duration

class InvalidDurationException(meetingIntentId: MeetingIntentId, duration: Duration) :
    DomainValidationException("MeetingIntent#$meetingIntentId: Unsupported duration '${duration.toMinutes()} min'")

class InvalidMeetingSlotException(meetingIntentId: MeetingIntentId, slot: MeetingSlot) :
    DomainValidationException("MeetingIntent#$meetingIntentId: Invalid slot '${slot.startTime} - ${slot.endTime}'")

class InvalidMeetingProposalException(meetingIntentId: MeetingIntentId, message: String) :
    DomainValidationException("MeetingIntent#$meetingIntentId cannot be proposed: $message")

class MeetingAlreadyConfirmedException(meetingProposalId: MeetingProposalId) :
    DomainValidationException("Meeting#$meetingProposalId already confirmed")

class MeetingProposalExpiredException(meetingProposalId: MeetingProposalId) :
    DomainValidationException("Meeting#$meetingProposalId has expired")

class InvalidMeetingStatusException(meetingProposalId: MeetingProposalId, status: MeetingStatus):
    DomainValidationException("Meeting#$meetingProposalId: Invalid status: $status")

class MeetingSlotUnavailableException(meetingProposalId: MeetingProposalId, slot: MeetingSlot):
    DomainValidationException("Meeting#$meetingProposalId: Slot unavailable: '${slot.startTime} - ${slot.endTime}'")
