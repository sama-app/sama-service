package com.sama.slotsuggestion.domain

import com.sama.common.DomainRepository
import org.springframework.cache.annotation.Cacheable
import java.time.ZonedDateTime

@DomainRepository
interface BlockRepository {
    fun findAllBlocks(
        userId: Long,
        startDateTime: ZonedDateTime,
        endDateTime: ZonedDateTime,
        includeRecurrence: Boolean = false
    ): Collection<Block>

    @Cacheable("blocks-by-user")
    fun findAllBlocksCached(userId: Long, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime): Collection<Block> {
        return findAllBlocks(userId, startDateTime, endDateTime, true)
    }
}