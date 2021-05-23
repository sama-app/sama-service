package com.sama.calendar.domain

import com.sama.users.domain.UserId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

class Meeting {

    @Id
    private var id: MeetingId? = null

    @Column(nullable = false)
    private var status: MeetingStatus = MeetingStatus.CREATED

    @Column(nullable = false)
    private var initiatorId: UserId? = null

    @Column(nullable = false)
    private var duration: Duration? = null

    private var recipientEmail: String? = null

    private val suggestedSlots: List<MeetingSlot> = mutableListOf()

    private var code: String? = null

    private val proposedSlots: List<MeetingSlot> = mutableListOf()

    private val confirmedSlot: MeetingSlot? = null

    @CreatedDate
    private var createdAt: Instant? = null

    @LastModifiedDate
    private var updatedAt: Instant? = null
}

enum class MeetingStatus {
    CREATED,
    PROPOSED,
    CONFIRMED,
    REJECTED,
    EXPIRED
}

data class MeetingSlot(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null,
    private val meetingId: MeetingId,
    val startDateTime: ZonedDateTime,
    val endDateTime: ZonedDateTime,
    val isRange: Boolean
)