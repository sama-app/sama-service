package com.sama.adapter

import com.google.api.client.http.GenericUrl
import com.sama.auth.domain.JwtPair
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.GoogleOauth2Failure
import com.sama.auth.application.GoogleOauth2Redirect
import com.sama.auth.application.GoogleOauth2Success
import liquibase.pro.packaged.it
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.lang.RuntimeException
import javax.servlet.http.HttpServletRequest


@RestController
class GoogleOauth2Controller internal constructor(
    private val googleOauth2ApplicationService: GoogleOauth2ApplicationService,
) {
    @PostMapping("/api/auth/google-authorize")
    fun googleAuthorize(request: HttpServletRequest): GoogleOauth2Redirect {
        val redirectUri = redirectUri(request)
        return googleOauth2ApplicationService.beginGoogleOauth2(redirectUri)
    }

    @GetMapping("/api/auth/google-oauth2")
    fun googleOauth2(
        request: HttpServletRequest,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
    ): GoogleOauth2Success {
        val redirectUri = redirectUri(request)
        val result = googleOauth2ApplicationService.processGoogleOauth2(redirectUri, code, error)

        when (result) {
            is GoogleOauth2Success -> return result
            is GoogleOauth2Failure -> throw RuntimeException(result.error) // TODO: learn to handle this
        }
    }

    private fun redirectUri(request: HttpServletRequest): String {
        return GenericUrl(request.requestURL.toString())
            .apply {
                this.rawPath = "/api/auth/google-oauth2"
                this.scheme = if (this.host != "localhost") "https" else "http"
            }
            .build()
    }
}