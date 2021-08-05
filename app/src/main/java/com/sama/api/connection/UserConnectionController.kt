package com.sama.api.connection

import com.sama.api.config.AuthUserId
import com.sama.connection.application.CreateConnectionRequestCommand
import com.sama.connection.application.RemoveUserConnectionCommand
import com.sama.connection.application.UserConnectionApplicationService
import com.sama.connection.domain.ConnectionRequestId
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@Tag(name = "connection")
@RestController
class UserConnectionController(private val userConnectionApplicationService: UserConnectionApplicationService) {

    @GetMapping(
        "/api/connection/user-connections",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun findConnections(@AuthUserId userId: UserId) =
        userConnectionApplicationService.findUserConnections(userId)

    @GetMapping(
        "/api/connection/requests",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun findConnectionRequests(@AuthUserId userId: UserId) =
        userConnectionApplicationService.findConnectionRequests(userId)

    @PostMapping(
        "/api/connection/create-request",
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createConnectionRequest(@AuthUserId userId: UserId, @RequestBody command: CreateConnectionRequestCommand) =
        userConnectionApplicationService.createConnectionRequest(userId, command)

    @PostMapping("/api/connection/request/{connectionRequestId}/approve")
    fun approveConnectionRequest(@AuthUserId userId: UserId, @PathVariable connectionRequestId: ConnectionRequestId) =
        userConnectionApplicationService.approveConnectionRequest(userId, connectionRequestId)


    @PostMapping("/api/connection/request/{connectionRequestId}/reject")
    fun rejectConnectionRequest(@AuthUserId userId: UserId, @PathVariable connectionRequestId: ConnectionRequestId) =
        userConnectionApplicationService.rejectConnectionRequest(userId, connectionRequestId)

    @PostMapping("/api/connection/user-connections/disconnect-user")
    fun disconnectUser(@AuthUserId userId: UserId, @RequestBody command: RemoveUserConnectionCommand) =
        userConnectionApplicationService.removeUserConnection(userId, command)
}