package com.sama.adapter

import com.auth0.jwt.JWT
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.sama.auth.configuration.AccessJwtConfiguration
import com.sama.auth.domain.AuthUserRepository
import com.sama.auth.domain.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class TestController(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val authUserRepository: AuthUserRepository,
    private val jwtConfiguration: AccessJwtConfiguration
) {

    @GetMapping("/")
    fun hello(): String {
        return "Hello, dear Sama user!"
    }

    @GetMapping("/api/calendar")
    fun getCalendar(request: HttpServletRequest): List<Event> {
        val jwt = request.getHeader("Authorization").split("Bearer ".toRegex()).toTypedArray()[1]
        val decodedJwt = Jwt(jwt, jwtConfiguration).decoded()
        val email = decodedJwt.subject

        val authUser = authUserRepository.findByEmail(email)

        val credential = googleAuthorizationCodeFlow.loadCredential(authUser?.id().toString())
        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        val service = Calendar.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName("Sama App")
            .build()

        // List the next 10 events from the primary calendar.
        val now = DateTime(System.currentTimeMillis())
        val events = service.events().list("primary")
            .setMaxResults(10)
            .setTimeMin(now)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()
        return events.items
    }
}