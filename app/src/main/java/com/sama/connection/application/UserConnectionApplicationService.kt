package com.sama.connection.application

import com.sama.common.ApplicationService
import com.sama.common.checkAccess
import com.sama.comms.application.CommsEventConsumer
import com.sama.connection.domain.ConnectionRequest
import com.sama.connection.domain.ConnectionRequestId
import com.sama.connection.domain.ConnectionRequestRepository
import com.sama.connection.domain.DiscoveredUserListRepository
import com.sama.connection.domain.UserAlreadyConnectedException
import com.sama.connection.domain.UserConnectedEvent
import com.sama.connection.domain.UserConnection
import com.sama.connection.domain.UserConnectionRepository
import com.sama.connection.domain.UserConnectionRequestCreatedEvent
import com.sama.connection.domain.UserConnectionRequestRejectedEvent
import com.sama.users.application.InternalUserService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserConnectionApplicationService(
    private val userService: InternalUserService,
    private val connectionRequestRepository: ConnectionRequestRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val discoveredUserListRepository: DiscoveredUserListRepository,
    private val commsEventConsumer: CommsEventConsumer,
    private val userConnectionViews: UserConnectionViews,
) : UserConnectionService {

    @Transactional(readOnly = true)
    override fun isConnected(userOneId: UserId, userTwoId: UserId): Boolean {
        return userConnectionRepository.exists(userOneId, userTwoId)
    }

    @Transactional(readOnly = true)
    override fun findUserConnections(userId: UserId): UserConnectionsDTO {
        val discoveredUsers = discoveredUserListRepository.findById(userId).discoveredUsers
        val connectedUsers = userConnectionRepository.findConnectedUserIds(userId)
        return userConnectionViews.renderUserConnections(discoveredUsers, connectedUsers)
    }

    @Transactional
    override fun createUserConnection(userId: UserId, command: CreateUserConnectionCommand): Boolean {
        val recipientId = command.recipientId
        if (isConnected(userId, recipientId)) {
            throw UserAlreadyConnectedException(userId, recipientId)
        }

        val userConnection = UserConnection(userId, recipientId)
        userConnectionRepository.save(userConnection)

        val event = UserConnectedEvent(userId, userConnection)
        commsEventConsumer.onUserConnected(event)

        return true
    }

    @Transactional
    override fun removeUserConnection(userId: UserId, command: RemoveUserConnectionCommand): Boolean {
        val recipientId = userService.translatePublicId(command.userId)
        userConnectionRepository.delete(UserConnection(userId, recipientId))
        return true
    }

    @Transactional
    override fun addDiscoveredUsers(userId: UserId, command: AddDiscoveredUsersCommand): Boolean {
        val discoveredUsers = discoveredUserListRepository.findById(userId)
        val discoveredUserIds = userService.findIdsByEmail(command.userEmails)

        val (updated, newUserIds) = discoveredUsers.addDiscoveredUsers(discoveredUserIds)

        if (newUserIds.isNotEmpty()) {
            discoveredUserListRepository.save(updated)
            // TODO: Send notification?
            return true
        }
        return false
    }

    @Transactional(readOnly = true)
    override fun findConnectionRequests(userId: UserId): ConnectionRequestsDTO {
        val initiatedConnectionRequests = connectionRequestRepository.findPendingByInitiatorId(userId)
        val pendingConnectionRequests = connectionRequestRepository.findPendingByRecipientId(userId)
        return userConnectionViews.renderConnectionRequests(initiatedConnectionRequests, pendingConnectionRequests)
    }

    @Transactional
    override fun createConnectionRequest(userId: UserId, command: CreateConnectionRequestCommand): ConnectionRequestDTO {
        val recipientId = userService.translatePublicId(command.recipientId)

        val connectionRequest = connectionRequestRepository.findPendingByUserIds(userId, recipientId)
        if (connectionRequest != null) {
            return userConnectionViews.renderConnectionRequest(connectionRequest)
        }

        if (isConnected(userId, recipientId)) {
            throw UserAlreadyConnectedException(userId, recipientId)
        }

        val newConnectionRequest = ConnectionRequest.new(userId, recipientId)
        connectionRequestRepository.save(newConnectionRequest)

        val event = UserConnectionRequestCreatedEvent(userId, newConnectionRequest)
        commsEventConsumer.onConnectionRequestCreated(event)

        return userConnectionViews.renderConnectionRequest(newConnectionRequest)
    }

    @Transactional
    override fun approveConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId): Boolean {
        val connectionRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)
        checkAccess(connectionRequest.recipientUserId == userId)
        { "User#${userId.id} does not have access to ConnectionRequest#$connectionRequestId" }

        val (approvedRequest, userConnection) = connectionRequest.approve()

        userConnectionRepository.save(userConnection)
        connectionRequestRepository.save(approvedRequest)

        val event = UserConnectedEvent(userId, userConnection)
        commsEventConsumer.onUserConnected(event)
        return true
    }

    @Transactional
    override fun rejectConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId): Boolean {
        val connectionRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)
        checkAccess(connectionRequest.recipientUserId == userId)
        { "User#${userId.id} does not have access to ConnectionRequest#$connectionRequestId" }

        val rejectedRequest = connectionRequest.reject()
        connectionRequestRepository.save(rejectedRequest)

        val event = UserConnectionRequestRejectedEvent(userId, rejectedRequest)
        commsEventConsumer.onConnectionRequestRejected(event)
        return true
    }
}