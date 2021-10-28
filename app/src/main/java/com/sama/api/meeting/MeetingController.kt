package com.sama.api.meeting

import com.sama.meeting.application.ConfirmMeetingCommand
import com.sama.meeting.application.ConnectWithMeetingInitiatorCommand
import com.sama.meeting.application.CreateFullAvailabilityLinkCommand
import com.sama.meeting.application.InitiateMeetingCommand
import com.sama.meeting.application.MeetingIntentDTO
import com.sama.meeting.application.MeetingService
import com.sama.meeting.application.ProposeMeetingCommand
import com.sama.meeting.application.ProposeNewMeetingSlotsCommand
import com.sama.meeting.application.UpdateMeetingTitleCommand
import com.sama.meeting.domain.MeetingCode
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
class MeetingController(private val meetingService: MeetingService) {

    @Operation(
        summary = "Initiate a meeting giving basic parameters",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/initiate",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE],
    )
    fun initiateMeeting(@RequestBody @Valid command: InitiateMeetingCommand): MeetingIntentDTO {
        return meetingService.dispatchInitiateMeetingCommand(command)
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
    fun proposeMeeting(@RequestBody command: ProposeMeetingCommand) =
        meetingService.proposeMeeting(command)

    @Operation(
        summary = "Propose a meeting with a slot selection",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/full-availability-link",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun fullAvailabilityLink(@RequestBody command: CreateFullAvailabilityLinkCommand) =
        meetingService.createFullAvailabilityLink(command)

    @Operation(
        summary = "Propose new slots for an existing sama to sama meeting",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/propose-new-slots",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun proposeNewMeetingSlots(@PathVariable meetingCode: MeetingCode, @RequestBody command: ProposeNewMeetingSlotsCommand) =
        meetingService.proposeNewMeetingSlots(meetingCode, command)

    @Operation(
        summary = "Connect with meeting initiator",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/connect",
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun connectWithInitiator(@PathVariable meetingCode: MeetingCode, @RequestBody command: ConnectWithMeetingInitiatorCommand) =
        meetingService.connectWithInitiator(meetingCode, command)

    @Operation(summary = "Retrieve meeting proposal details using a shared meeting code")
    @GetMapping(
        "/api/meeting/by-code/{meetingCode}",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun loadMeetingProposal(@PathVariable meetingCode: MeetingCode) =
        meetingService.loadMeetingProposal(meetingCode)

    @Operation(summary = "Get slot suggestions for a proposed meeting")
    @GetMapping(
        "/api/meeting/by-code/{meetingCode}/suggestions",
        produces = [APPLICATION_JSON_VALUE]
    )
    fun getSlotSuggestions(@PathVariable meetingCode: MeetingCode) =
        meetingService.getSlotSuggestions(meetingCode)

    @Operation(
        summary = "Update meeting title to be used on the confirmed calendar event",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/update-title",
        consumes = [APPLICATION_JSON_VALUE],
    )
    fun updateMeetingTitle(@PathVariable meetingCode: MeetingCode, @RequestBody command: UpdateMeetingTitleCommand) =
        meetingService.updateMeetingTitle(meetingCode, command)

    @Operation(summary = "Confirm a meeting using a meeting code")
    @PostMapping(
        "/api/meeting/by-code/{meetingCode}/confirm",
        consumes = [APPLICATION_JSON_VALUE]
    )
    fun confirmMeeting(@PathVariable meetingCode: MeetingCode, @RequestBody command: ConfirmMeetingCommand) =
        meetingService.confirmMeeting(meetingCode, command)
}

