package com.sama.meeting.application

import com.sama.meeting.domain.MeetingCode
import com.sama.users.domain.UserId

interface MeetingService {
    fun dispatchInitiateMeetingCommand(command: InitiateMeetingCommand): MeetingIntentDTO
    fun proposeMeeting(command: ProposeMeetingCommand): MeetingInvitationDTO
    fun createFullAvailabilityLink(command: CreateFullAvailabilityLinkCommand): String
    fun loadMeetingProposal(meetingCode: MeetingCode): ProposedMeetingDTO
    fun proposeNewMeetingSlots(meetingCode: MeetingCode, command: ProposeNewMeetingSlotsCommand): Boolean
    fun getSlotSuggestions(meetingCode: MeetingCode): MeetingSlotSuggestionDTO
    fun updateMeetingTitle(meetingCode: MeetingCode, command: UpdateMeetingTitleCommand): Boolean
    fun connectWithInitiator(meetingCode: MeetingCode, command: ConnectWithMeetingInitiatorCommand): Boolean
    fun confirmMeeting(meetingCode: MeetingCode, command: ConfirmMeetingCommand): Boolean
}