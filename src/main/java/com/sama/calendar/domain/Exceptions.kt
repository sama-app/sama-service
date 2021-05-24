package com.sama.calendar.domain

import java.time.Duration


class UnsupportedDurationException(meetingId: MeetingId, duration: Duration) :
    RuntimeException("Unsupported duration '${duration.toMinutes()} min' for Meeting#$meetingId")

class InvalidSuggestedSlotException(meetingId: MeetingId, slot: MeetingSlot) :
    RuntimeException("Invalid slot '${slot.startTime} - ${slot.endTime}' for Meeting#$meetingId")