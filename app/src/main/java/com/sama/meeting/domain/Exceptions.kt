package com.sama.meeting.domain

import com.sama.common.DomainEntityStatusException
import com.sama.common.DomainInvalidActionException
import com.sama.common.DomainIntegrityException
import com.sama.common.DomainValidationException
import java.time.Duration

class InvalidDurationException(meetingIntentId: MeetingIntentId, duration: Duration) :
    DomainValidationException("MeetingIntent#$meetingIntentId: Unsupported duration '${duration.toMinutes()} min'")

class InvalidMeetingSlotException(meetingIntentId: MeetingIntentId, slot: MeetingSlot) :
    DomainValidationException("MeetingIntent#$meetingIntentId: Invalid slot '${slot.startDateTime} - ${slot.endDateTime}'")

class InvalidMeetingProposalException(meetingIntentId: MeetingIntentId, message: String) :
    DomainValidationException("MeetingIntent#$meetingIntentId cannot be proposed: $message")

class MeetingAlreadyConfirmedException(meetingId: MeetingId) :
    DomainEntityStatusException("already_confirmed", "Meeting#$meetingId already confirmed")

class MeetingProposalExpiredException(meetingId: MeetingId) :
    DomainEntityStatusException("proposal_expired", "Meeting#$meetingId has expired")

class InvalidMeetingStatusException(meetingId: MeetingId, status: MeetingStatus):
    DomainEntityStatusException("invalid_status", "Meeting#$meetingId: Invalid status: $status")

class MeetingSlotUnavailableException(meetingId: MeetingId, slot: MeetingSlot):
    DomainInvalidActionException("slot_unavailable", "Meeting#$meetingId: Slot unavailable: '${slot.startDateTime} - ${slot.endDateTime}'")
