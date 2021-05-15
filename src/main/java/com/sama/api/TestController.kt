package com.sama.api

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.sama.api.common.UserId
import com.sama.common.NotFoundException
import com.sama.users.domain.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    private val googleAuthorizationCodeFlow: GoogleAuthorizationCodeFlow,
    private val firebaseApp: FirebaseApp,
    private val userRepository: UserRepository
) {

    @GetMapping("/api/test/")
    fun hello(): String {
        return "Hello, dear Sama user!"
    }

    @GetMapping("/api/test/calendar")
    fun getCalendar(@UserId userId: Long): List<Event> {
        val credential = googleAuthorizationCodeFlow.loadCredential(userId.toString())
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

    @PostMapping("/api/test/send-push")
    fun sendPush(@UserId userId: Long, @RequestBody command: SendPushCommand): String {
        val authUser = userRepository.findByIdOrNull(userId)
            ?: throw NotFoundException(userId)
        return authUser.sendPushNotification(command.message, FirebaseMessaging.getInstance(firebaseApp))
    }
}

data class SendPushCommand(val message: String)