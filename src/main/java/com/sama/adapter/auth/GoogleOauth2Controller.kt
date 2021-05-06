package com.sama.adapter.auth

import com.google.api.client.http.GenericUrl
import com.sama.auth.domain.JwtPair
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.GoogleOauth2Failure
import com.sama.auth.application.GoogleOauth2Redirect
import com.sama.auth.application.GoogleOauth2Success
import liquibase.pro.packaged.it
import org.springframework.mobile.device.DeviceUtils
import org.springframework.security.core.Authentication
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import java.lang.RuntimeException
import javax.servlet.http.HttpServletRequest


@RestController
class GoogleOauth2Controller(
    private val googleOauth2ApplicationService: GoogleOauth2ApplicationService,
) {
    @PostMapping("/api/auth/google-authorize")
    fun googleAuthorize(authentication: Authentication?, request: HttpServletRequest): GoogleOauth2Redirect {
        val redirectUri = redirectUri(request)
        return googleOauth2ApplicationService.beginGoogleOauth2(redirectUri)
    }

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
                        redirectView.url = "yoursama://auth/success"
                    }

                    currentDevice.isNormal -> {
                        val genericUrl = GenericUrl(request.requestURL.toString())
                        genericUrl.rawPath = "/api/auth/success"
                        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
                        redirectView.url = genericUrl.build()
                    }
                }

                redirectView
            }

            is GoogleOauth2Failure -> {
                val redirectView = RedirectView()
                redirectView.attributesMap["error"] = result.error

                when {
                    currentDevice.isMobile || currentDevice.isTablet -> {
                        redirectView.url = "yoursama://auth/error"
                    }

                    currentDevice.isNormal -> {
                        val genericUrl = GenericUrl(request.requestURL.toString())
                        genericUrl.rawPath = "/api/auth/error"
                        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
                        redirectView.url = genericUrl.build()
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