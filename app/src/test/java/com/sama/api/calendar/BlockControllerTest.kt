package com.sama.api.calendar

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.calendar.application.BlockApplicationService
import com.sama.calendar.application.BlockDTO
import com.sama.calendar.application.FetchBlocksDTO
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        BlockController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class BlockControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var blockApplicationService: BlockApplicationService

    private val userId: Long = 1
    private val jwt = "eyJraWQiOiJkdW1teS1hY2Nlc3Mta2V5LWlkLWZvci1kZXZlbG9wbWVudCIsInR5cCI6IkpXVCIsImFsZyI6IkhTMjU2" +
            "In0.eyJzdWIiOiJiYWx5cytzYW1hQHZhbGVudHVrZXZpY2l1cy5jb20iLCJleHAiOjE2MjI5MTM4NjQsImlhdCI6MTYyMDMyMTg2NC" +
            "wianRpIjoiYTk5MDNiOTEtNjc1ZC00NDExLTg3YjQtZjFhMTk3Y2FjZjdhIn0.kO4SeU-4OO61U0UfkQsAnZW0l1ntjhHy7_k6JhRY" +
            "zg8"


    @Test
    fun `fetch blocks with valid dates`() {
        val startDate = LocalDate.of(2021, 1, 1)
        val endDate = LocalDate.of(2021, 1, 2)
        val zoneId = ZoneId.of("Europe/Rome")

        val startDateTime = ZonedDateTime.of(startDate, LocalTime.of(12, 15), zoneId)
        val endDateTime = ZonedDateTime.of(startDate, LocalTime.of(12, 30), zoneId)
        whenever(blockApplicationService.fetchBlocks(eq(userId), eq(startDate), eq(endDate), eq(zoneId)))
            .thenReturn(FetchBlocksDTO(listOf(BlockDTO(startDateTime, endDateTime, false, "test"))))

        val expectedJson = """
        {
            "blocks": [
                {
                    "startDateTime": "2021-01-01T12:15:00+01:00",
                    "endDateTime": "2021-01-01T12:30:00+01:00",
                    "allDay": false,
                    "title": "test"
                }
            ]
        }
        """

        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-01")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `fetch blocks empty`() {
        val startDate = LocalDate.of(2021, 1, 1)
        val endDate = LocalDate.of(2021, 1, 2)
        val zoneId = ZoneId.of("Europe/Rome")

        whenever(blockApplicationService.fetchBlocks(eq(userId), eq(startDate), eq(endDate), eq(zoneId)))
            .thenReturn(FetchBlocksDTO(emptyList()))

        val expectedJson = """
        {
            "blocks": [
            ]
        }
        """

        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-01")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isOk)
            .andExpect(content().json(expectedJson, true))
    }

    @Test
    fun `fetch blocks with non-iso dates fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "01/01/2021")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `fetch blocks with endDate before startDate fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-03")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `fetch blocks with invalid timezone fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .header("Authorization", "Bearer $jwt")
                .queryParam("startDate", "2021-01-03")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Invalid")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `fetch blocks without authorization fails`() {
        mockMvc.perform(
            get("/api/calendar/blocks")
                .queryParam("startDate", "2021-01-01")
                .queryParam("endDate", "2021-01-02")
                .queryParam("timezone", "Europe/Rome")
        )
            .andExpect(status().isForbidden)
    }
}