package com.sama.meeting.domain

import com.sama.common.DomainEvent
import com.sama.users.domain.UserId

@DomainEvent
data class MeetingProposedEvent(val actorId: UserId, val proposedMeeting: ProposedMeeting)

@DomainEvent
data class NewMeetingSlotsProposedEvent(val actorId: UserId, val proposedMeeting: ProposedMeeting)

@DomainEvent
data class MeetingConfirmedEvent(val actorId: UserId?, val confirmedMeeting: ConfirmedMeeting)