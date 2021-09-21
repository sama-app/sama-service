package com.sama.connection.application

import com.sama.common.ApplicationService
import com.sama.connection.domain.ConnectionRequest
import com.sama.connection.domain.ConnectionRequestId
import com.sama.connection.domain.ConnectionRequestRepository
import com.sama.connection.domain.DiscoveredUserListRepository
import com.sama.connection.domain.UserConnection
import com.sama.connection.domain.UserConnectionRepository
import com.sama.users.application.InternalUserService
import com.sama.users.domain.UserId
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserConnectionApplicationService(
    private val userService: InternalUserService,
    private val connectionRequestRepository: ConnectionRequestRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val discoveredUserListRepository: DiscoveredUserListRepository,
    private val userConnectionViews: UserConnectionViews,
) : UserConnectionService {

    @Transactional(readOnly = true)
    override fun isConnected(userOneId: UserId, userTwoId: UserId): Boolean {
        TODO("Not yet implemented")
    }

    @Transactional(readOnly = true)
    override fun findUserConnections(userId: UserId): UserConnectionsDTO {
        val discoveredUsers = discoveredUserListRepository.findById(userId).discoveredUsers
        val connectedUsers = userConnectionRepository.findConnectedUserIds(userId)
        return userConnectionViews.renderUserConnections(discoveredUsers, connectedUsers)
    }

    @Transactional
    override fun createConnection(createConnectionCommand: CreateConnectionCommand): Boolean {
        TODO("Not yet implemented")
    }

    @Transactional(readOnly = true)
    override fun findConnectionRequests(userId: UserId): ConnectionRequestsDTO {
        val initiatedConnectionRequests = connectionRequestRepository.findPendingByInitiatorId(userId)
        val pendingConnectionRequests = connectionRequestRepository.findPendingByRecipientId(userId)
        return userConnectionViews.renderConnectionRequests(initiatedConnectionRequests, pendingConnectionRequests)
    }

    @Transactional
    override fun createConnectionRequest(initiatorId: UserId, command: CreateConnectionRequestCommand): ConnectionRequestDTO {
        val recipientId = userService.translatePublicId(command.recipientId)

        val connectionRequest = connectionRequestRepository.findPendingByUserIds(initiatorId, recipientId)
        if (connectionRequest != null) {
            return userConnectionViews.renderConnectionRequest(connectionRequest)
        }

        val connectedUserIds = userConnectionRepository.findConnectedUserIds(initiatorId)

        val newConnectionRequest = ConnectionRequest.new(initiatorId, recipientId, connectedUserIds)
        connectionRequestRepository.save(newConnectionRequest)

        // TODO: send comms
        return userConnectionViews.renderConnectionRequest(newConnectionRequest)
    }

    @Transactional
    override fun approveConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId) {
        val connectionRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)
        if (connectionRequest.recipientUserId != userId) {
            throw AccessDeniedException("User#${userId.id} does not have access to ConnectionRequest#$connectionRequestId")
        }

        val (approvedRequest, userConnection) = connectionRequest.approve()

        userConnectionRepository.save(userConnection)
        connectionRequestRepository.save(approvedRequest)
        // TODO: send comms
    }

    @Transactional
    override fun rejectConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId) {
        val connectionRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)
        if (connectionRequest.recipientUserId != userId) {
            throw AccessDeniedException("User#${userId.id} does not have access to ConnectionRequest#$connectionRequestId")
        }

        val rejectedRequest = connectionRequest.reject()
        connectionRequestRepository.save(rejectedRequest)
        // TODO: send comms?
    }

    @Transactional
    override fun removeUserConnection(userId: UserId, command: RemoveUserConnectionCommand) {
        val recipientId = userService.translatePublicId(command.userId)
        userConnectionRepository.delete(UserConnection(userId, recipientId))
    }
}