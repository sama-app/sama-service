package com.sama.slotsuggestion.domain

import com.sama.common.DomainRepository
import com.sama.users.domain.UserId
import org.springframework.cache.annotation.Cacheable
import java.time.ZonedDateTime

@DomainRepository
interface BlockRepository {
    fun findAllBlocks(
        userId: UserId,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        includeRecurrence: Boolean = false
    ): Collection<Block>

    @Cacheable("blocks-by-user")
    fun findAllBlocksCached(userId: UserId, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<Block> {
        return findAllBlocks(userId, startDateTime, endDateTime, true)
    }
}