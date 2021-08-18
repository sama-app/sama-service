package com.sama.connection.application

import com.sama.common.ApplicationService
import com.sama.connection.domain.*
import com.sama.users.domain.UserId
import com.sama.users.infrastructure.jpa.UserJpaRepository
import com.sama.users.infrastructure.jpa.findIdByPublicIdOrThrow
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserConnectionApplicationService(
    private val userRepository: UserJpaRepository,
    private val connectionRequestRepository: ConnectionRequestRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val discoveredUserListRepository: DiscoveredUserListRepository,
    private val userConnectionViews: UserConnectionViews,
) {

    @Transactional(readOnly = true)
    fun findUserConnections(userId: UserId): UserConnectionsDTO {
        val discoveredUsers = discoveredUserListRepository.findById(userId).discoveredUsers
        val connectedUsers = userConnectionRepository.findConnectedUserIds(userId)
        return userConnectionViews.renderUserConnections(discoveredUsers, connectedUsers)
    }

    @Transactional(readOnly = true)
    fun findConnectionRequests(userId: UserId): ConnectionRequestsDTO {
        val initiatedConnectionRequests = connectionRequestRepository.findPendingByInitiatorId(userId)
        val pendingConnectionRequests = connectionRequestRepository.findPendingByRecipientId(userId)
        return userConnectionViews.renderConnectionRequests(initiatedConnectionRequests, pendingConnectionRequests)
    }

    @Transactional
    fun createConnectionRequest(initiatorId: UserId, command: CreateConnectionRequestCommand): ConnectionRequestDTO {
        val recipientId = userRepository.findIdByPublicIdOrThrow(command.recipientId)

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
    @PreAuthorize("@auth.hasRecipientAccess(#userId, #connectionRequestId)")
    fun approveConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId) {
        val connectionRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)

        val (approvedRequest, userConnection) = connectionRequest.approve()

        userConnectionRepository.save(userConnection)
        connectionRequestRepository.save(approvedRequest)
        // TODO: send comms
    }

    @Transactional
    @PreAuthorize("@auth.hasRecipientAccess(#userId, #connectionRequestId)")
    fun rejectConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId) {
        val rejectedRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)
            .reject()
        connectionRequestRepository.save(rejectedRequest)
        // TODO: send comms?
    }

    @Transactional
    fun removeUserConnection(userId: UserId, command: RemoveUserConnectionCommand) {
        val recipientId = userRepository.findIdByPublicIdOrThrow(command.userId)
        userConnectionRepository.delete(UserConnection(userId, recipientId))
    }
}