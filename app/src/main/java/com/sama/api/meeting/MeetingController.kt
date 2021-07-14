package com.sama.api.meeting

import com.sama.api.config.AuthUserId
import com.sama.meeting.application.*
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingIntentId
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Tag(name = "meeting")
@RestController
class MeetingController(
    private val meetingApplicationService: MeetingApplicationService
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
    fun initiateMeeting(@AuthUserId userId: UserId, @RequestBody @Valid command: InitiateMeetingCommand) =
        meetingApplicationService.initiateMeeting(userId, command)

    @Operation(
        summary = "Propose a meeting with a slot selection",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/{meetingIntentId}/propose",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun proposeMeeting(
        @AuthUserId userId: UserId,
        @PathVariable meetingIntentId: MeetingIntentId,
        @RequestBody command: ProposeMeetingCommand
    ) = meetingApplicationService.proposeMeeting(userId, meetingIntentId, command)

    @Operation(
        summary = "Propose a meeting with a slot selection",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/propose",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun proposeMeetingV2(
        @AuthUserId userId: UserId,
        @RequestBody command: ProposeMeetingCommandV2
    ) = meetingApplicationService.proposeMeeting(userId, command)


    @Operation(summary = "Retrieve meeting proposal details using a shared meeting code")
    @GetMapping(
        "/api/meeting/by-code/{meetingCode}",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun loadMeetingProposal(@PathVariable meetingCode: MeetingCode) =
        meetingApplicationService.loadMeetingProposalFromCode(meetingCode)

    @Operation(summary = "Confirm a meeting using a meeting code")
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/confirm",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun confirmMeeting(@PathVariable meetingCode: MeetingCode, @RequestBody command: ConfirmMeetingCommand) =
        meetingApplicationService.confirmMeeting(meetingCode, command)
}