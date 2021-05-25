package com.sama.api.calendar

import com.sama.api.config.AuthUserId
import com.sama.calendar.application.ConfirmMeetingCommand
import com.sama.calendar.application.InitiateMeetingCommand
import com.sama.calendar.application.MeetingApplicationService
import com.sama.calendar.application.ProposeMeetingCommand
import com.sama.calendar.domain.MeetingId
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun initiateMeeting(@AuthUserId userId: UserId, @RequestBody command: InitiateMeetingCommand) =
        meetingApplicationService.initiateMeeting(userId, command)

    @Operation(
        summary = "Propose a meeting with a slot selection",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/{meetingId}/propose",
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun proposeMeeting(
        @AuthUserId userId: UserId,
        @RequestParam meetingId: MeetingId,
        @RequestBody command: ProposeMeetingCommand
    ) = meetingApplicationService.proposeMeeting(userId, meetingId, command)

    @Operation(
        summary = "Confirm a meeting using a meeting code",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/meeting/{meetingId}/confirm",
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun confirmMeeting(@AuthUserId userId: UserId, @RequestBody command: ConfirmMeetingCommand) =
        meetingApplicationService.confirmMeeting(userId, command)
}