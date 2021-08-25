package com.sama.api.meeting

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.meeting.application.ConfirmMeetingCommand
import com.sama.meeting.application.InitiateMeetingCommand
import com.sama.meeting.application.MeetingAppLinksDTO
import com.sama.meeting.application.MeetingApplicationService
import com.sama.meeting.application.MeetingDTO
import com.sama.meeting.application.MeetingIntentDTO
import com.sama.meeting.application.MeetingInvitationDTO
import com.sama.meeting.application.MeetingSlotDTO
import com.sama.meeting.application.ProposeMeetingCommand
import com.sama.meeting.application.ProposedMeetingDTO
import com.sama.meeting.application.UpdateMeetingTitleCommand
import com.sama.meeting.domain.InvalidMeetingStatusException
import com.sama.meeting.domain.MeetingAlreadyConfirmedException
import com.sama.meeting.domain.MeetingCode
import com.sama.meeting.domain.MeetingIntentCode
import com.sama.meeting.domain.MeetingStatus
import com.sama.users.application.UserPublicDTO
import com.sama.users.domain.UserId
import com.sama.users.domain.UserPublicId
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.isEqualTo

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        MeetingController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class MeetingControllerTest(@Autowired val mockMvc: MockMvc) {

    @MockBean
    lateinit var meetingApplicationService: MeetingApplicationService

    private val userId = UserId(1)
    private val jwt = "eyJraWQiOiJrZXktaWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJiYWx5c0B5b3Vyc2FtYS5jb20iLCJ1c2VyX2lkIjoiNjViOTc3ZWEtODk4MC00YjFhLWE2ZWUtZjhmY2MzZjFmYzI0Iiwi" +
            "ZXhwIjoxNjIyNTA1NjYwLCJpYXQiOjE2MjI1MDU2MDAsImp0aSI6IjNlNWE3NTY3LWZmYmQtNDcxYi1iYTI2LTU2YjMwOTgwMWZlZSJ9." +
            "hcAQ6f8kaeB43nzFibGYZE8QWHyz9OIdFg9zHSbe9Vk"

    @Test
    fun `initiate meeting`() {
        val durationMinutes = 30L
        val timeZone = ZoneId.of("Europe/Rome")
        val slotSuggestionCount = 3
        val code = MeetingIntentCode.random()

        whenever(
            meetingApplicationService.initiateMeeting(
                userId, InitiateMeetingCommand(durationMinutes, timeZone, slotSuggestionCount)
            )
        ).thenReturn(
            MeetingIntentDTO(
                code,
                durationMinutes,
                listOf(
                    MeetingSlotDTO(
                        ZonedDateTime.parse("2021-01-01T12:00:00Z"),
                        ZonedDateTime.parse("2021-01-01T13:00:00Z"),
                    )
                )
            )
        )

        val requestBody = """
            {
                "durationMinutes": $durationMinutes,
                "timeZone": "${timeZone.id}",
                "suggestionSlotCount": $slotSuggestionCount
            }
        """

        val expectedResponse = """
            {
                "meetingIntentCode": "${code.code}",
                "durationMinutes": $durationMinutes,
                "suggestedSlots":[
                    {
                        "startDateTime": "2021-01-01T12:00:00Z",
                        "endDateTime": "2021-01-01T13:00:00Z"
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
            .andExpect(content().json(expectedResponse))
    }

    @TestFactory
    fun `initiate meeting request validation return 400`() = listOf(
        """
            {
                "durationMinutes": 14,
                "timeZone": "Europe/Rome",
                "suggestionSlotCount": 3
            }
        """,
        """
            {
                "durationMinutes": 30,
                "timeZone": "Europe/Rome",
                "suggestionSlotCount": -1
            }
        """
    )
        .mapIndexed { idx, requestBody ->
            dynamicTest("request#$idx") {
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
        val initiatorId = UserPublicId.random()
        val initiatorFullName = "test"
        val initiatorEmail = "test@meetsama.com"
        val meetingIntentCode = MeetingIntentCode.random()
        val shareableMessage = "a nice message"
        val meetingCode = MeetingCode("code")
        val meetingUrl = "localhost:3000/$meetingCode"
        val meetingTitle = "Meeting with test"
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.parse("2021-01-01T12:00:00Z[UTC]"),
            ZonedDateTime.parse("2021-01-01T13:00:00Z[UTC]"),
        )
        whenever(
            meetingApplicationService.proposeMeeting(
                userId, ProposeMeetingCommand(
                    meetingIntentCode,
                    listOf(proposedSlot)
                )
            )
        ).thenReturn(
            MeetingInvitationDTO(
                MeetingDTO(
                    listOf(proposedSlot),
                    UserPublicDTO(initiatorId, initiatorFullName, initiatorEmail),
                    meetingTitle
                ),
                meetingCode,
                meetingUrl,
                shareableMessage
            )
        )

        val requestBody = """
            {
                "meetingIntentCode": "${meetingIntentCode.code}",
                "proposedSlots": [{
                    "startDateTime": "2021-01-01T12:00:00Z",
                    "endDateTime": "2021-01-01T13:00:00Z"
                }]
            }
        """

        val expectedResponse = """
            {
                "meeting": {
                    "proposedSlots": [{
                        "startDateTime": "2021-01-01T12:00:00Z",
                        "endDateTime": "2021-01-01T13:00:00Z"
                     }],
                    "initiator": {
                        "userId": ${initiatorId.id},
                        "fullName": $initiatorFullName,
                        "email": $initiatorEmail
                    },
                    "title": "$meetingTitle"
                },
                "meetingCode": ${meetingCode.code},
                "meetingUrl": "$meetingUrl",
                "shareableMessage": "$shareableMessage"
            }
        """
        mockMvc.perform(
            post("/api/meeting/propose")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedResponse))
    }


    @Test
    fun `load meeting proposal`() {
        val userId = UserPublicId.random()
        val meetingCode = MeetingCode("code")
        val meetingTitle = "Meeting title"
        val proposedSlot = MeetingSlotDTO(
            ZonedDateTime.parse("2021-01-01T12:00:00Z"),
            ZonedDateTime.parse("2021-01-01T13:00:00Z"),
        )
        whenever(meetingApplicationService.loadMeetingProposal(meetingCode))
            .thenReturn(
                ProposedMeetingDTO(
                    listOf(proposedSlot),
                    UserPublicDTO(userId, "test", "test@meetsama.com"),
                    meetingTitle,
                    MeetingAppLinksDTO("http://download.me")
                )
            )

        val expectedResponse = """
            {
                "proposedSlots": [{
                    "startDateTime": "2021-01-01T12:00:00Z",
                    "endDateTime": "2021-01-01T13:00:00Z"
                 }],
                "initiator": {
                    "userId": ${userId.id},
                    "fullName": "test",
                    "email": "test@meetsama.com"
                },
                "title": "$meetingTitle",
                "appLinks": {
                    "iosAppDownloadLink": "http://download.me"
                }
            }
        """
        mockMvc.perform(
            get("/api/meeting/by-code/${meetingCode.code}")
                .header("Authorization", "Bearer $jwt")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedResponse))
    }

    @Test
    fun `update meeting title`() {
        val meetingCode = MeetingCode("VGsUTGno")
        val meetingTitle = "my fancy title"

        whenever(meetingApplicationService.updateMeetingTitle(
            userId, meetingCode, UpdateMeetingTitleCommand(meetingTitle))
        ).thenReturn(true)


        val requestBody =
            """
                {
                    "title": "$meetingTitle"
                }
            """.trimIndent()

        mockMvc.perform(
            post("/api/meeting/by-code/${meetingCode.code}/update-title")
                .contentType(APPLICATION_JSON)
                .content(requestBody)
                .header("Authorization", "Bearer $jwt"))
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `meeting already confirmed`() {
        val meetingCode = MeetingCode("VGsUTGno")
        whenever(meetingApplicationService.loadMeetingProposal(meetingCode))
            .thenThrow(MeetingAlreadyConfirmedException(meetingCode))

        val expectedResponse = """
       {
            "status": 410,
            "reason": "already_confirmed"
        }
        """
        mockMvc.perform(get("/api/meeting/by-code/${meetingCode.code}"))
            .andExpect(status().isGone)
            .andExpect(content().json(expectedResponse))
    }

    @Test
    fun `meeting status invalid`() {
        whenever(meetingApplicationService.loadMeetingProposal(MeetingCode("VGsUTGno")))
            .thenThrow(InvalidMeetingStatusException(MeetingCode("VGsUTGno"), MeetingStatus.REJECTED))

        val expectedResponse = """
       {
            "status": 410,
            "reason": "invalid_status"
        }
        """
        mockMvc.perform(get("/api/meeting/by-code/VGsUTGno"))
            .andExpect(status().isGone)
            .andExpect(content().json(expectedResponse))
    }

    @Test
    fun `confirm meeting unauthenticated`() {
        val meetingCode = MeetingCode("VGsUTGno")
        val recipientEmail = "lucky@sama.com"

        val command = ConfirmMeetingCommand(
            slot = MeetingSlotDTO(
                ZonedDateTime.parse("2021-01-01T12:00:00Z[UTC]"),
                ZonedDateTime.parse("2021-01-01T13:00:00Z[UTC]")
            ),
            recipientEmail = recipientEmail
        )
        whenever(meetingApplicationService.confirmMeeting(null, meetingCode, command))
            .thenReturn(true)

        val requestBody = """
            {
                "slot": {
                    "startDateTime": "2021-01-01T12:00:00Z",
                    "endDateTime": "2021-01-01T13:00:00Z"
                },
                "recipientEmail": "$recipientEmail"
            }
        """

        mockMvc.perform(
            post("/api/meeting/by-code/${meetingCode.code}/confirm")
                .contentType(APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `confirm meeting authenticated`() {
        val meetingCode = MeetingCode("VGsUTGno")
        val command = ConfirmMeetingCommand(
            slot = MeetingSlotDTO(
                ZonedDateTime.parse("2021-01-01T12:00:00Z[UTC]"),
                ZonedDateTime.parse("2021-01-01T13:00:00Z[UTC]")
            ),
            recipientEmail = null
        )
        whenever(meetingApplicationService.confirmMeeting(userId, meetingCode, command))
            .thenReturn(true)

        val requestBody = """
            {
                "slot": {
                    "startDateTime": "2021-01-01T12:00:00Z",
                    "endDateTime": "2021-01-01T13:00:00Z"
                }
            }
        """

        mockMvc.perform(
            post("/api/meeting/by-code/${meetingCode.code}/confirm")
                .contentType(APPLICATION_JSON)
                .header("Authorization", "Bearer $jwt")
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `confirm meeting unauthenticated without recipient email`() {
        val meetingCode = MeetingCode("VGsUTGno")
        val command = ConfirmMeetingCommand(
            slot = MeetingSlotDTO(
                ZonedDateTime.parse("2021-01-01T12:00:00Z[UTC]"),
                ZonedDateTime.parse("2021-01-01T13:00:00Z[UTC]")
            ),
            recipientEmail = null
        )
        whenever(meetingApplicationService.confirmMeeting(null, meetingCode, command))
            .thenReturn(true)

        val requestBody = """
            {
                "slot": {
                    "startDateTime": "2021-01-01T12:00:00Z",
                    "endDateTime": "2021-01-01T13:00:00Z"
                }
            }
        """

        mockMvc.perform(
            post("/api/meeting/by-code/${meetingCode.code}/confirm")
                .contentType(APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @TestFactory
    fun `endpoint authorization without jwt`() = listOf(
        post("/api/meeting/initiate") to UNAUTHORIZED,
        post("/api/meeting/1/propose") to UNAUTHORIZED,
        get("/api/meeting/by-code/some-code") to OK,
        post("/api/meeting/by-code/some-code/update-title") to UNAUTHORIZED,
        post("/api/meeting/by-code/some-code/confirm")
            .contentType(APPLICATION_JSON) to BAD_REQUEST, // no payload
    )
        .mapIndexed { idx, (request, expectedStatus) ->
            dynamicTest("request#$idx returns $expectedStatus") {
                mockMvc.perform(request)
                    .andExpect(status().isEqualTo(expectedStatus.value()))
            }
        }
}