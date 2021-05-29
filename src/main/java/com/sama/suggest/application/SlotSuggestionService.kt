package com.sama.suggest.application

import com.sama.common.ApplicationService
import com.sama.users.domain.UserId
import org.springframework.stereotype.Service
import java.time.*

@ApplicationService
@Service
class SlotSuggestionService {

    fun suggestSlots(userId: UserId, request: SlotSuggestionRequest): List<SlotSuggestion> {
        val now = ZonedDateTime.of(LocalDateTime.of(LocalDate.now(), LocalTime.of(1, 0, 0)), ZoneId.systemDefault())
        return listOf(
            SlotSuggestion(now, now.plusHours(1)),
            SlotSuggestion(now.plusHours(2), now.plusHours(3))
        )
    }
}

data class SlotSuggestionRequest(
    val count: Int,
    val duration: Duration,
    val timezone: ZoneId
)

data class SlotSuggestion(
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime
)