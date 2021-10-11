package com.sama.integration.google.calendar.domain

import com.sama.integration.google.auth.domain.GoogleAccountId
import com.sama.integration.google.calendar.application.GoogleChannelNotification
import com.sama.integration.google.calendar.domain.ChannelStatus.CLOSED
import com.sama.integration.google.calendar.domain.ChannelStatus.CREATED
import com.sama.integration.google.calendar.domain.ChannelStatus.SYNCING
import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.crypto.keygen.KeyGenerators


typealias GoogleChannel = com.google.api.services.calendar.model.Channel

enum class ChannelStatus {
    CREATED,
    SYNCING,
    CLOSED
}

enum class ResourceType {
    CALENDAR_LIST,
    CALENDAR
}

/**
 * A channel that receives push notification from Google Calendar API when the specified
 * resource is updated.
 * https://developers.google.com/calendar/api/guides/push
 */
@Table
data class Channel(
    @Id @Column("id")
    val channelId: UUID,
    val googleAccountId: GoogleAccountId,
    val status: ChannelStatus,
    val resourceType: ResourceType,
    /**
     * Internal resource id. `null` for account level resources, such as [CalendarList]
     */
    val resourceId: String?,
    /**
     * Arbitrary String sent to Google when creating the Channel. Should be verified
     * on every received message
     */
    val token: String,
    /**
     * Id of the channel target resource in the Google systems
     */
    val externalResourceId: String,
    /**
     * Timestamp when the Channel expires and a new one must be created
     */
    val expiresAt: Instant,
    /**
     * Number of the last processed message
     */
    val messageNumber: Long,
    val updatedAt: Instant
) : Persistable<UUID> {

    companion object {
        private val tokenGenerator = KeyGenerators.string()

        fun newId(): UUID = UUID.randomUUID()

        fun newToken() = "check=" + tokenGenerator.generateKey()

        fun new(
            id: UUID,
            accountId: GoogleAccountId,
            token: String,
            resourceType: ResourceType,
            resourceId: String?,
            externalResourceId: String?,
            expiresAt: Instant
        ) =
            Channel(
                id,
                accountId,
                CREATED,
                resourceType,
                resourceId,
                token,
                externalResourceId ?: "",
                expiresAt,
                -1,
                Instant.now()
            )
    }

    fun receiveMessage(notification: GoogleChannelNotification): Channel {
        check(notification.token == token)
        if (status == CLOSED) {
            throw ChannelClosedException(id)
        }
        return copy(status = SYNCING, messageNumber = notification.messageNumber, updatedAt = Instant.now())
    }

    fun close(): Channel {
        return copy(status = CLOSED)
    }

    fun debugString(): String {
        return "GoogleAccount${googleAccountId.id} ${resourceType}: ${resourceId}..."
    }

    // spring-data-jdbc overrides
    override fun isNew() = status == CREATED
    override fun getId() = channelId
}