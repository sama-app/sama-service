package com.sama.integration.google.calendar.domain

import com.sama.common.DomainEvent
import com.sama.users.domain.UserId
import java.time.LocalDate


@DomainEvent
data class CalendarDatesUpdatedEvent(val userId: UserId, val dates: Set<LocalDate>)