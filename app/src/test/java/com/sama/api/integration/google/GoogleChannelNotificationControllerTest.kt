package com.sama.api.integration.google

import com.sama.api.ApiTestConfiguration
import com.sama.api.config.WebMvcConfiguration
import com.sama.integration.google.calendar.application.GoogleChannelNotification
import com.sama.integration.google.calendar.application.GoogleChannelNotificationReceiver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [
        GoogleChannelNotificationController::class,
        WebMvcConfiguration::class,
        ApiTestConfiguration::class
    ]
)
@AutoConfigureMockMvc
class GoogleChannelNotificationControllerTest(
    @Autowired val mockMvc: MockMvc
) {
    @MockBean
    lateinit var notificationReceiver: GoogleChannelNotificationReceiver

    @Test
    fun `receive notification`() {
        val channelId = "4ba78bf0-6a47-11e2-bcfd-0800200c9a66"
        val token = "check=398348u3tu83ut8uu38&test=true"
        val resourceId = "ret08u3rv24htgh289g"
        val resourceState = "exists"
        val messageNumber = 10L
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/integration/google/channel-notification")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Channel-ID", channelId)
                .header("X-Goog-Channel-Token", token)
                .header("X-Goog-Channel-Expiration", "Tue, 19 Nov 2013 01:13:52 GMT")
                .header("X-Goog-Resource-ID", resourceId)
                .header("X-Goog-Resource-URI", "https://www.googleapis.com/calendar/v3/calendars/my_calendar@gmail.com/events")
                .header("X-Goog-Resource-State", resourceState)
                .header("X-Goog-Message-Number", messageNumber)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(notificationReceiver)
            .receive(GoogleChannelNotification(channelId, token, resourceId, resourceState, messageNumber))
    }
}