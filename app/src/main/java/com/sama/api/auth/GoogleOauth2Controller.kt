package com.sama.api.auth

import com.google.api.client.http.GenericUrl
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.GoogleSignErrorDTO
import com.sama.auth.application.GoogleSignSuccessDTO
import com.sama.auth.application.LinkGoogleAccountErrorDTO
import com.sama.auth.application.LinkGoogleAccountSuccessDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView


@Tag(name = "auth")
@RestController
class GoogleOauth2Controller(
    private val googleOauth2ApplicationService: GoogleOauth2ApplicationService,
) {

    @Operation(summary = "Start Google Web OAuth2 process")
    @PostMapping("/api/auth/google-authorize")
    fun googleAuthorize(request: HttpServletRequest) =
        googleOauth2ApplicationService.generateAuthorizationUrl(redirectUri(request))

    @Operation(
        summary = "Callback for Google OAuth2 process",
        responses = [
            ApiResponse(
                responseCode = "302",
                description = "Platform specific redirect URI with a authentication JWT pair",
                content = [Content(schema = Schema(hidden = true))]
            )
        ],
    )
    @GetMapping("/api/auth/google-oauth2")
    fun googleOauth2Callback(
        request: HttpServletRequest,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?,
    ): RedirectView {
        val redirectUri = redirectUri(request)
        val result = googleOauth2ApplicationService.processOauth2Callback(redirectUri, code, error, state)

        return when (result) {
            is GoogleSignSuccessDTO -> {
                val redirectView = RedirectView()
                redirectView.attributesMap["accessToken"] = result.accessToken
                redirectView.attributesMap["refreshToken"] = result.refreshToken
                redirectView.url = "meetsama://auth/success"
                redirectView
            }
            is GoogleSignErrorDTO -> {
                val redirectView = RedirectView()
                redirectView.attributesMap["reason"] = result.error
                redirectView.url = "meetsama://auth/error"
                redirectView
            }
            is LinkGoogleAccountSuccessDTO -> {
                val redirectView = RedirectView()
                redirectView.url = "meetsama://link-google-account/success"
                redirectView.attributesMap["accountId"] = result.googleAccountId.id
                redirectView
            }
            is LinkGoogleAccountErrorDTO -> {
                val redirectView = RedirectView()
                redirectView.attributesMap["reason"] = result.error
                redirectView.url = "meetsama://link-google-account/error"
                redirectView
            }
        }
    }

    private fun redirectUri(request: HttpServletRequest): String {
        val genericUrl = GenericUrl(request.requestURL.toString())
        genericUrl.rawPath = "/api/auth/google-oauth2"
        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
        return genericUrl.build()
    }
}