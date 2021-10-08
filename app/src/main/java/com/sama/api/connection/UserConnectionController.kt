package com.sama.api.connection

import com.sama.api.config.AuthUserId
import com.sama.connection.application.CreateConnectionRequestCommand
import com.sama.connection.application.RemoveUserConnectionCommand
import com.sama.connection.application.UserConnectionService
import com.sama.connection.domain.ConnectionRequestId
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@Tag(name = "connection")
@RestController
class UserConnectionController(private val userConnectionService: UserConnectionService) {

    @Operation(
        summary = "List connected and discovered users for the current user",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        "/api/connection/user-connections",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun findConnections(@AuthUserId userId: UserId?) =
        userConnectionService.findUserConnections(userId!!)


    @Operation(
        summary = "List pending and initiated connection requests for the current user",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping(
        "/api/connection/requests",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun findConnectionRequests(@AuthUserId userId: UserId?) =
        userConnectionService.findConnectionRequests(userId!!)

    @Operation(
        summary = "Create a connection request",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping(
        "/api/connection/request/create",
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createConnectionRequest(@AuthUserId userId: UserId?, @RequestBody command: CreateConnectionRequestCommand) =
        userConnectionService.createConnectionRequest(userId!!, command)

    @Operation(
        summary = "Approve a connection request",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping("/api/connection/request/{connectionRequestId}/approve")
    fun approveConnectionRequest(@AuthUserId userId: UserId?, @PathVariable connectionRequestId: ConnectionRequestId) =
        userConnectionService.approveConnectionRequest(userId!!, connectionRequestId)

    @Operation(
        summary = "Reject a connection request",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping("/api/connection/request/{connectionRequestId}/reject")
    fun rejectConnectionRequest(@AuthUserId userId: UserId?, @PathVariable connectionRequestId: ConnectionRequestId) =
        userConnectionService.rejectConnectionRequest(userId!!, connectionRequestId)

    @Operation(
        summary = "Remove a user from your connected users",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping("/api/connection/user-connections/disconnect-user")
    fun disconnectUser(@AuthUserId userId: UserId?, @RequestBody command: RemoveUserConnectionCommand) =
        userConnectionService.removeUserConnection(userId!!, command)
}