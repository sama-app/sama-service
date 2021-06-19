package com.sama.meeting.application

import com.sama.meeting.configuration.MeetingProposalMessageModel
import com.sama.meeting.configuration.MeetingUrlConfiguration
import com.sama.meeting.domain.MeetingInvitation
import com.sama.meeting.domain.ProposedMeeting
import com.samskivert.mustache.Template
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class MeetingInvitationServiceTest(
    @Mock private val template: Template,
) {

    @Test
    fun `find for proposed meeting`() {
        val scheme = "https"
        val host = "sama.com"
        val meetingCode = "code"
        val configuration = MeetingUrlConfiguration(10, scheme, host)

        val shareableMessage = "invitation message"
        whenever(template.execute(any())).thenReturn(shareableMessage)

        val underTest = MeetingInvitationService(configuration, template)

        // act
        val meetingInvitation = underTest.findForProposedMeeting(
            ProposedMeeting(
                21L, 11L, 1L, Duration.ofMinutes(15), listOf(), meetingCode
            )
        )

        // verify
        val expectedResponse = MeetingInvitation("$scheme://$host/$meetingCode", shareableMessage)
        assertEquals(expectedResponse, meetingInvitation)

        val expectedModel = MeetingProposalMessageModel(listOf(), "$scheme://$host/$meetingCode")
        verify(template).execute(eq(expectedModel))
    }
}