package com.sama.api.meeting

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.meeting.application.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.isEqualTo
import java.time.ZoneId
import java.time.ZonedDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        MeetingController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class MeetingControllerTes(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var meetingApplicationService: MeetingApplicationService

    private val userId: Long = 1
    private val jwt = "eyJraWQiOiJkdW1teS1hY2Nlc3Mta2V5LWlkLWZvci1kZXZlbG9wbWVudCIsInR5cCI6IkpXVCIsImFsZyI6IkhTMjU2" +
            "In0.eyJzdWIiOiJiYWx5cytzYW1hQHZhbGVudHVrZXZpY2l1cy5jb20iLCJleHAiOjE2MjI5MTM4NjQsImlhdCI6MTYyMDMyMTg2NC" +
            "wianRpIjoiYTk5MDNiOTEtNjc1ZC00NDExLTg3YjQtZjFhMTk3Y2FjZjdhIn0.kO4SeU-4OO61U0UfkQsAnZW0l1ntjhHy7_k6JhRY" +
            "zg8"


    @Test
    fun `initiate meeting`() {
        val durationMinutes = 30L
        val timeZone = ZoneId.of("Europe/Rome")
        val slotSuggestionCount = 3
        val suggestionDayCount = 21

        whenever(
            meetingApplicationService.initiateMeeting(
                eq(userId),
                eq(InitiateMeetingCommand(durationMinutes, timeZone, slotSuggestionCount, suggestionDayCount))
            )
        ).thenReturn(
            MeetingIntentDTO(
                11L,
                userId,
                RecipientDTO(null, null),
                30,
                listOf(
                    MeetingSlotDTO(
                        ZonedDateTime.parse("2021-01-01T12:00:00+01:00"),
                        ZonedDateTime.parse("2021-01-01T13:00:00+01:00"),
                    )
                )
            )
        )

        val requestBody = """
            {
                "durationMinutes": $durationMinutes,
                "timeZone": "${timeZone.id}",
                "suggestionSlotCount": $slotSuggestionCount,
                "suggestionDayCount": $suggestionDayCount
            }
        """

        val expectedResponse = """
            {
                "meetingIntentId": 11,
                "initiatorId": 1,
                "recipient": {
                  "userId": null,
                  "email": null
                },
                "suggestedSlots":[
                    {
                        "startDateTime": "2021-01-01T12:00:00+01:00",
                        "endDateTime": "2021-01-01T13:00:00+01:00"
                    }
                ]
            }
        """
        mockMvc.perform(
            post("/api/meeting/initiate")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(expectedResponse))
    }

    @TestFactory
    fun `initiate meeting request validation return 400`() = listOf(
        """
            {
                "durationMinutes": 14,
                "timeZone": "Europe/Rome",
                "suggestionSlotCount": 3,
                "suggestionDayCount": 21
            }
        """,
        """
            {
                "durationMinutes": 30,
                "timeZone": "Europe/Rome",
                "suggestionSlotCount": -1,
                "suggestionDayCount": 21
            }
        """,
        """
            {
                "durationMinutes": 30,
                "timeZone": "Europe/Rome",
                "suggestionSlotCount": 1,
                "suggestionDayCount": -1
            }
        """
    )
        .mapIndexed { idx, requestBody ->
            DynamicTest.dynamicTest("request#$idx") {
                mockMvc.perform(
                    post("/api/meeting/initiate")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer $jwt")
                        .content(requestBody)
                )
                    .andExpect(status().isBadRequest)
            }
        }


    @Test
    fun `propose meeting`() {
        val meetingIntentId = 11L
        val meetingId = 21L
        val meetingCode = "code"
        val shareableMessage = "a nice message"
        val meetingUrl = "localhost:3000/code"
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.parse("2021-01-01T12:00:00+01:00"),
            ZonedDateTime.parse("2021-01-01T13:00:00+01:00"),
        )
        whenever(
            meetingApplicationService.proposeMeeting(
                eq(userId), eq(meetingIntentId), any()
            )
        ).thenReturn(
            MeetingInvitationDTO(
                ProposedMeetingDTO(
                    meetingId,
                    listOf(proposedSlot),
                    meetingCode
                ),
                meetingUrl,
                shareableMessage
            )
        )

        val requestBody = """
            {
                "proposedSlots": [{
                    "startDateTime": "2021-01-01T12:00:00+01:00",
                    "endDateTime": "2021-01-01T13:00:00+01:00"
                }]
            }
        """

        val expectedResponse = """
            {
                "meeting": {
                    "meetingId": $meetingId,
                    "meetingCode": "$meetingCode",
                    "proposedSlots": [{
                        "startDateTime": "2021-01-01T12:00:00+01:00",
                        "endDateTime": "2021-01-01T13:00:00+01:00"
                     }]
                },
                "meetingUrl": "$meetingUrl",
                "shareableMessage": "$shareableMessage"
            }
        """
        mockMvc.perform(
            post("/api/meeting/$meetingIntentId/propose")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(expectedResponse))
    }

    @Test
    fun `load meeting proposal`() {
        val meetingCode = "code"
        val meetingId = 21L
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.parse("2021-01-01T12:00:00+01:00"),
            ZonedDateTime.parse("2021-01-01T13:00:00+01:00"),
        )
        whenever(
            meetingApplicationService.loadMeetingProposalFromCode(eq(meetingCode))
        ).thenReturn(ProposedMeetingDTO(meetingId, listOf(proposedSlot), meetingCode))

        val expectedResponse = """
            {
                "meetingId": $meetingId,
                "meetingCode": "$meetingCode",
                "proposedSlots": [{
                    "startDateTime": "2021-01-01T12:00:00+01:00",
                    "endDateTime": "2021-01-01T13:00:00+01:00"
                 }]
            }
        """
        mockMvc.perform(
            get("/api/meeting/by-code/$meetingCode")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(expectedResponse))
    }

    @Test
    fun `confirm meeting`() {
        val meetingCode = "code"
        val recipientEmail = "lucky@sama.com"

        whenever(meetingApplicationService.confirmMeeting(eq(meetingCode), any()))
            .thenReturn(true)

        val requestBody = """
            {
                "slot": {
                    "startDateTime": "2021-01-01T12:00:00+01:00",
                    "endDateTime": "2021-01-01T13:00:00+01:00"
                },
                "recipientEmail": "$recipientEmail"
            }
        """

        mockMvc.perform(
            post("/api/meeting/by-code/$meetingCode/confirm")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("true"))
    }


    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        post("/api/meeting/initiate") to FORBIDDEN,
        post("/api/meeting/1/propose") to FORBIDDEN,
        get("/api/meeting/by-code/some-code") to OK,
        post("/api/meeting/by-code/some-code/confirm")
            .contentType(APPLICATION_JSON) to BAD_REQUEST, // no payload
    )
        .mapIndexed { idx, (request, expectedStatus) ->
            DynamicTest.dynamicTest("request#$idx returns $expectedStatus") {
                mockMvc.perform(request)
                    .andExpect(status().isEqualTo(expectedStatus.value()))
            }
        }
}