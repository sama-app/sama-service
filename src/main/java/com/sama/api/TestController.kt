package com.sama.api

import com.sama.calendar.application.InitiateMeetingCommand
import com.sama.calendar.application.MeetingApplicationService
import com.sama.common.NotFoundException
import com.sama.users.domain.UserEntity
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class TestController(
    private val meetingApplicationService: MeetingApplicationService
) {

    @GetMapping("/api/test/")
    fun hello(): String {
        meetingApplicationService.initiateMeeting(3, InitiateMeetingCommand(60, "test@test.com"))
        throw NotFoundException(UserEntity::class, 1)
//        return "Hello, dear Sama user!"
    }
}
