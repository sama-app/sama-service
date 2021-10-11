package com.sama.integration.google.calendar.domain

import com.sama.integration.google.auth.domain.GoogleAccountId
import java.time.Instant
import java.util.UUID
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ChannelRepository : CrudRepository<Channel, UUID> {
    fun findByGoogleAccountIdAndResourceType(googleAccountId: GoogleAccountId, resourceType: ResourceType): List<Channel>
    fun findByExpiresAtLessThan(expiredAt: Instant): List<Channel>

    @Modifying
    @Query("DELETE FROM gcal.channel c WHERE c.status = 'CLOSED'")
    fun deleteAllClosed(): Int
}