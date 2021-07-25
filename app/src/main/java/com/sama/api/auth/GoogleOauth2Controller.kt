package com.sama.api.auth

import com.google.api.client.http.GenericUrl
import com.sama.users.application.GoogleOauth2ApplicationService
import com.sama.users.application.GoogleOauth2Failure
import com.sama.users.application.GoogleOauth2Success
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.mobile.device.DeviceUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.HttpServletRequest


@Tag(name = "auth")
@RestController
class GoogleOauth2Controller(
    private val googleOauth2ApplicationService: GoogleOauth2ApplicationService,
) {

    @Operation(summary = "Start google OAuth2 process")
    @PostMapping("/api/auth/google-authorize")
    fun googleAuthorize(request: HttpServletRequest) =
        googleOauth2ApplicationService.beginGoogleOauth2(redirectUri(request))


    @Operation(
        summary = "Callback for google OAuth2 process",
        responses = [
            ApiResponse(
                responseCode = "302",
                description = "Platform specific redirect URI with a authentication JWT pair",
                content = [Content(schema = Schema(hidden = true))]
            )
        ],
    )
    @GetMapping("/api/auth/google-oauth2")
    fun googleOauth2(
        request: HttpServletRequest,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
    ): RedirectView {
        val redirectUri = redirectUri(request)
        val result = googleOauth2ApplicationService.processGoogleOauth2(redirectUri, code, error)
        val currentDevice = DeviceUtils.getCurrentDevice(request)

        return when (result) {
            is GoogleOauth2Success -> {
                val redirectView = RedirectView()
                redirectView.attributesMap["accessToken"] = result.accessToken
                redirectView.attributesMap["refreshToken"] = result.refreshToken

                when {
                    currentDevice.isMobile || currentDevice.isTablet -> {
                        redirectView.url = "meetsama://auth/success"
                    }

                    currentDevice.isNormal -> {
                        redirectView.url = "meetsama://auth/success"
//                        val genericUrl = GenericUrl(request.requestURL.toString())
//                        genericUrl.rawPath = "/api/auth/success"
//                        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
//                        redirectView.url = genericUrl.build()
                    }
                }

                redirectView
            }

            is GoogleOauth2Failure -> {
                val redirectView = RedirectView()
                redirectView.attributesMap["reason"] = result.error

                when {
                    currentDevice.isMobile || currentDevice.isTablet -> {
                        redirectView.url = "meetsama://auth/error"
                    }

                    currentDevice.isNormal -> {
                        redirectView.url = "meetsama://auth/error"
//                        val genericUrl = GenericUrl(request.requestURL.toString())
//                        genericUrl.rawPath = "/api/auth/error"
//                        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
//                        redirectView.url = genericUrl.build()
                    }
                }
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