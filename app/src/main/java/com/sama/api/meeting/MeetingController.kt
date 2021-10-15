package com.sama.api.meeting

import com.sama.api.config.AuthUserId
import com.sama.meeting.application.ConfirmMeetingCommand
import com.sama.meeting.application.ConnectWithMeetingInitiatorCommand
import com.sama.meeting.application.CreateFullAvailabilityLinkCommand
import com.sama.meeting.application.InitiateMeetingCommand
import com.sama.meeting.application.MeetingApplicationService
import com.sama.meeting.application.MeetingIntentDTO
import com.sama.meeting.application.ProposeMeetingCommand
import com.sama.meeting.application.ProposeNewMeetingSlotsCommand
import com.sama.meeting.application.UpdateMeetingTitleCommand
import com.sama.meeting.domain.MeetingCode
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import javax.validation.Valid
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "meeting")
@RestController
class MeetingController(
    private val meetingApplicationService: MeetingApplicationService,
) {

    @Operation(
        summary = "Initiate a meeting giving basic parameters",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/initiate",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE],
    )
    fun initiateMeeting(@AuthUserId userId: UserId?, @RequestBody @Valid command: InitiateMeetingCommand): MeetingIntentDTO {
        return meetingApplicationService.dispatchInitiateMeetingCommand(userId!!, command)
    }

    @Operation(
        summary = "Propose a meeting with a slot selection",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/propose",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun proposeMeeting(
        @AuthUserId userId: UserId?,
        @RequestBody command: ProposeMeetingCommand,
    ) = meetingApplicationService.proposeMeeting(userId!!, command)

    @Operation(
        summary = "Propose a meeting with a slot selection",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/full-availability-link",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun fullAvailabilityLink(
        @AuthUserId userId: UserId?,
        @RequestBody command: CreateFullAvailabilityLinkCommand,
    ) = meetingApplicationService.createFullAvailabilityLink(userId!!, command)

    @Operation(
        summary = "Propose new slots for an existing sama to sama meeting",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/propose-new-slots",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun proposeNewMeetingSlots(
        @AuthUserId userId: UserId?,
        @PathVariable meetingCode: MeetingCode,
        @RequestBody command: ProposeNewMeetingSlotsCommand,
    ) = meetingApplicationService.proposeNewMeetingSlots(userId!!, meetingCode, command)

    @Operation(
        summary = "Connect with meeting initiator",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/connect",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun connectWithInitiator(
        @AuthUserId userId: UserId?,
        @PathVariable meetingCode: MeetingCode,
        @RequestBody command: ConnectWithMeetingInitiatorCommand,
    ) = meetingApplicationService.connectWithInitiator(userId!!, meetingCode, command)

    @Operation(summary = "Retrieve meeting proposal details using a shared meeting code")
    @GetMapping(
        "/api/meeting/by-code/{meetingCode}",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun loadMeetingProposal(@AuthUserId userId: UserId?, @PathVariable meetingCode: MeetingCode) =
        meetingApplicationService.loadMeetingProposal(userId, meetingCode)


    @Operation(summary = "Get slot suggestions for a proposed meeting")
    @GetMapping(
        "/api/meeting/by-code/{meetingCode}/suggestions",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun getSlotSuggestions(@AuthUserId userId: UserId?, @PathVariable meetingCode: MeetingCode) =
        meetingApplicationService.getSlotSuggestions(userId!!, meetingCode)

    @Operation(
        summary = "Update meeting title to be used on the confirmed calendar event",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/update-title",
        consumes = [APPLICATION_JSON_VALUE],
    )
    fun updateMeetingTitle(
        @AuthUserId userId: UserId?,
        @PathVariable meetingCode: MeetingCode,
        @RequestBody command: UpdateMeetingTitleCommand,
    ) = meetingApplicationService.updateMeetingTitle(userId!!, meetingCode, command)

    @Operation(summary = "Confirm a meeting using a meeting code")
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/confirm",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun confirmMeeting(
        @AuthUserId userId: UserId?,
        @PathVariable meetingCode: MeetingCode,
        @RequestBody command: ConfirmMeetingCommand,
    ): Boolean {
        require(userId != null || command.recipientEmail != null)
        return meetingApplicationService.confirmMeeting(userId, meetingCode, command)
    }
}

