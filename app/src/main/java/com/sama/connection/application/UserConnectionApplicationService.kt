package com.sama.connection.application

import com.sama.common.ApplicationService
import com.sama.connection.domain.CalendarContactFinder
import com.sama.connection.domain.ConnectionRequestId
import com.sama.connection.domain.ConnectionRequestRepository
import com.sama.connection.domain.UserConnectionsRepository
import com.sama.users.domain.UserId
import com.sama.users.domain.UserRepository
import com.sama.users.domain.findIdByEmailOrThrow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.ZonedDateTime
import liquibase.pro.packaged.it
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@ApplicationService
@Service
class UserConnectionApplicationService(
    private val calendarContactFinder: CalendarContactFinder,
    private val userRepository: UserRepository,
    private val userConnectionsRepository: UserConnectionsRepository,
    private val connectionRequestRepository: ConnectionRequestRepository,
    private val clock: Clock
) {
    private var logger: Logger = LoggerFactory.getLogger(UserConnectionApplicationService::class.java)

    @Transactional(readOnly = true)
    fun findUserConnections(userId: UserId): UserConnectionsDTO {
        val userConnections = userConnectionsRepository.findByIdOrThrow(userId)

        val userIdToBasicDetails = userRepository.findBasicDetailsById(userConnections.allUserIds())
            .associateBy { it.id }

        return UserConnectionsDTO(
            userConnections.connectedUsers.mapNotNull { connectionUserId ->
                userIdToBasicDetails[connectionUserId]?.toUserConnectionDTO()
            },
            userConnections.discoveredUsers.mapNotNull { connectionUserId ->
                userIdToBasicDetails[connectionUserId]?.toUserConnectionDTO()
            },
        )
    }

    @Transactional
    fun createConnectionRequest(initiatorId: UserId, command: CreateConnectionRequestCommand) {
        val recipientId = userRepository.findIdByEmailOrThrow(command.recipientEmail)
        if (connectionRequestRepository.existsPendingByUserIds(initiatorId, recipientId)) {
            return
        }
        val userConnections = userConnectionsRepository.findByIdOrThrow(initiatorId)

        val (updatedUserConnections, connectionRequest) = userConnections.createConnectionRequest(recipientId)

        if (userConnections != updatedUserConnections) {
            userConnectionsRepository.save(updatedUserConnections)
        }
        connectionRequestRepository.save(connectionRequest)

        // TODO: send comms
    }

    // TODO: Verify the user is the recipient
    fun approveConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId) {
        val connectionRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)
        val userConnections = userConnectionsRepository.findByIdOrThrow(connectionRequest.initiatorId)

        val (updatedUserConnections, updatedConnectionRequest) = userConnections.approve(connectionRequest)

        connectionRequestRepository.save(updatedConnectionRequest)
        if (userConnections != updatedUserConnections) {
            userConnectionsRepository.save(userConnections)
            // TODO: send comms
        }
    }

    // TODO: Verify the user is the recipient
    fun rejectConnectionRequest(userId: UserId, connectionRequestId: ConnectionRequestId) {
        val connectionRequest = connectionRequestRepository.findByIdOrThrow(connectionRequestId)
        val userConnections = userConnectionsRepository.findByIdOrThrow(connectionRequest.initiatorId)

        val (_, updatedConnectionRequest) = userConnections.reject(connectionRequest)

        if (connectionRequest != updatedConnectionRequest) {
            connectionRequestRepository.save(updatedConnectionRequest)
            // TODO: send comms?
        }
    }

    fun discoverUsers(userId: UserId): Set<UserId> {
        // TODO check permissions

        val currentDateTime = ZonedDateTime.now(clock)
        val lastScanDateTime = currentDateTime.minusYears(1)
        val scannedContactEmails = calendarContactFinder.scanForContacts(userId, lastScanDateTime, currentDateTime)
            .map { it.email }.toSet()
        val discoveredUserIds = userRepository.findIdsByEmail(scannedContactEmails)

        val userConnections = userConnectionsRepository.findByIdOrThrow(userId)
            .addDiscoveredUsers(discoveredUserIds)

        userConnectionsRepository.save(userConnections)

        return discoveredUserIds
    }
}