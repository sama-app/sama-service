package com.sama.api

import com.sama.common.NotFoundException
import com.sama.users.domain.UserEntity
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class TestController {

    @GetMapping("/api/test/")
    fun hello(): String {
        throw NotFoundException(UserEntity::class, 1)
//        return "Hello, dear Sama user!"
    }
}
