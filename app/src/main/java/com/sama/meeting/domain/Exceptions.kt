package com.sama.meeting.domain

import com.sama.common.DomainEntityStatusException
import com.sama.common.DomainInvalidActionException
import com.sama.common.DomainValidationException
import java.time.Duration

class InvalidDurationException(duration: Duration) :
    DomainValidationException("Unsupported duration '${duration.toMinutes()} min'")

class InvalidMeetingSlotException(slot: MeetingSlot) :
    DomainValidationException("Invalid slot '${slot.startDateTime} - ${slot.endDateTime}'")

class InvalidMeetingProposalException(message: String) :
    DomainValidationException("Meeting cannot be proposed: $message")

class InvalidMeetingInitiationException(message: String) :
    DomainValidationException("Meeting cannot be initiated: $message")

class MeetingAlreadyConfirmedException(meetingCode: MeetingCode) :
    DomainEntityStatusException("already_confirmed", "Meeting#${meetingCode.code} already confirmed")

class InvalidMeetingStatusException(meetingCode: MeetingCode, status: MeetingStatus):
    DomainEntityStatusException("invalid_status", "Meeting#$meetingCode.code: Invalid status: $status")

class MeetingSlotUnavailableException(meetingCode: MeetingCode, slot: MeetingSlot):
    DomainInvalidActionException("slot_unavailable", "Meeting#$meetingCode.code: Slot unavailable: '${slot.startDateTime} - ${slot.endDateTime}'")
