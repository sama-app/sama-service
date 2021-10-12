package com.sama.meeting.application

import com.sama.common.View
import com.sama.meeting.configuration.MeetingProposalMessageModel
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.configuration.toUrl
import com.sama.meeting.domain.MeetingSlot
import com.sama.meeting.domain.ProposedMeeting
import com.sama.users.application.UserService
import com.sama.users.application.UserSettingsDTO
import com.sama.users.application.UserSettingsService
import com.samskivert.mustache.Template
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofLocalizedTime
import java.time.format.FormatStyle.SHORT
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.TimeZone
import org.springframework.stereotype.Component

@View
@Component
class MeetingInvitationView(
    private val userService: UserService,
    private val userSettingsService: UserSettingsService,
    private val meetingUrlConfiguration: MeetingUrlConfiguration,
    private val meetingProposalMessageTemplate: Template,
) {
    private val dateFormatterTemplate = DateTimeFormatter.ofPattern("EEE, MMMM d")
    private val timeFormatterTemplate = ofLocalizedTime(SHORT)

    fun render(proposedMeeting: ProposedMeeting, recipientTimeZone: ZoneId): MeetingInvitationDTO {
        val initiator = userService.find(proposedMeeting.initiatorId)
        val initiatorSettings = userSettingsService.find(proposedMeeting.initiatorId)
        val sortedProposedSots = proposedMeeting.proposedSlots
            .map { it.atTimeZone(recipientTimeZone) }
            .sortedBy { it.startDateTime }
        val meetingUrl = proposedMeeting.meetingCode.toUrl(meetingUrlConfiguration)

        val shareableMessage = renderShareableMessage(
            sortedProposedSots,
            meetingUrl,
            initiatorSettings.timeZone,
            initiatorSettings.locale,
            recipientTimeZone
        )

        return MeetingInvitationDTO(
            MeetingDTO(
                sortedProposedSots.map
                { it.toDTO() },
                initiator,
                proposedMeeting.meetingTitle
            ),
            proposedMeeting.meetingCode,
            meetingUrl,
            shareableMessage
        )
    }

    fun renderShareableMessage(
        slots: List<MeetingSlot>,
        meetingUrl: String,
        initiatorTimeZone: ZoneId,
        initiatorLocale: Locale,
        recipientTimeZone: ZoneId
    ): String {
        val (dateFormatter, timeFormatter) = dateTimeFormatters(initiatorTimeZone, initiatorLocale, recipientTimeZone)
        val sortedProposedSots = slots
            .map { it.atTimeZone(recipientTimeZone) }
            .sortedBy { it.startDateTime }
        val (showTimeZone, timeZone) = timeZone(initiatorTimeZone, recipientTimeZone)

        return meetingProposalMessageTemplate.execute(
            MeetingProposalMessageModel(
                showTimeZone, timeZone,
                sortedProposedSots
                    .groupBy { it.startDateTime.toLocalDate() }
                    .mapKeys { (date, _) -> dateFormatter.format(date).removeYear() }
                    .mapValues { (_, slots) ->
                        slots.map { it.formatTimeRange(timeFormatter) }
                    }.entries,
                meetingUrl
            )
        )
    }

    private fun dateTimeFormatters(initiatorTimeZone: ZoneId, initiatorLocale: Locale, recipientTimeZone: ZoneId) =
        when {
            initiatorTimeZone != recipientTimeZone -> {
                dateFormatterTemplate.withLocale(ENGLISH) to
                timeFormatterTemplate
                        .withLocale(initiatorLocale)
                        .withZone(recipientTimeZone)
            }
            else -> {
                dateFormatterTemplate.withLocale(ENGLISH) to
                        timeFormatterTemplate.withLocale(initiatorLocale)
            }
        }

    private fun timeZone(initiatorTimeZone: ZoneId, recipientTimeZone: ZoneId): Pair<Boolean, String> {
        val showTimeZone = initiatorTimeZone != recipientTimeZone
        // val daylightSaving = recipientTimeZone.rules.isDaylightSavings(sortedProposedSots.first().startDateTime.toInstant())
        val daylightSaving = false
        val timeZone = TimeZone.getTimeZone(recipientTimeZone).getDisplayName(daylightSaving, TimeZone.SHORT, ENGLISH)
        return showTimeZone to timeZone
    }

    private fun MeetingSlot.formatTimeRange(timeFormatter: DateTimeFormatter): String {
        val startTime = timeFormatter.format(startDateTime)
        val endTime = timeFormatter.format(endDateTime)
        return "$startTime - $endTime"
    }

    private fun String.removeYear(): String {
        return this.replace(Regex("(,)? \\d{4}(,)?"), "")
    }
}