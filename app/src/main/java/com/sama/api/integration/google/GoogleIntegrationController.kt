package com.sama.api.integration.google

import com.google.api.client.http.GenericUrl
import com.sama.api.config.AuthUserId
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.LinkGoogleAccountErrorDTO
import com.sama.auth.application.LinkGoogleAccountSuccessDTO
import com.sama.integration.google.auth.application.GoogleAccountApplicationService
import com.sama.integration.google.auth.application.UnlinkGoogleAccountCommand
import com.sama.users.domain.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView


@Tag(name = "integration.google")
@RestController
class GoogleIntegrationController(
    private val googleOauth2ApplicationService: GoogleOauth2ApplicationService,
    private val googleAccountService: GoogleAccountApplicationService
) {

    @Operation(
        summary = "Get a list of linked Google Accounts",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @GetMapping("/api/integration/google")
    fun listLinkedGoogleAccounts(@AuthUserId userId: UserId?) =
        googleAccountService.findAllLinked(userId!!)

    @Operation(
        summary = "Link an additional Google Calendar account using an OAuth2 token",
        responses = [
            ApiResponse(
                responseCode = "302",
                description = """Platform specific redirect URI: 
                    meetsama://integration/google/link-account/success OR
                    meetsama://integration/google/link-account/error?reason={}
                """,
                content = [Content(schema = Schema(hidden = true))]
            )
        ],
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping("/api/integration/google/link-account")
    fun linkGoogleAccount(@AuthUserId userId: UserId?, request: HttpServletRequest) =
        googleOauth2ApplicationService.generateAuthorizationUrl(redirectUri(request), userId)

    @Operation(
        summary = "Unlink an additional Google Calendar account",
        security = [SecurityRequirement(name = "user-auth")]
    )
    @PostMapping("/api/integration/google/unlink-account")
    fun unlinkGoogleAccount(@AuthUserId userId: UserId?, @RequestBody command: UnlinkGoogleAccountCommand) =
        googleAccountService.unlinkAccount(userId!!, command)

    @Operation(
        summary = "Callback for Google OAuth2 process",
        responses = [
            ApiResponse(responseCode = "302", content = [Content(schema = Schema(hidden = true))])
        ],
    )
    @GetMapping("/api/integration/google/callback")
    fun googleOauth2Callback(
        request: HttpServletRequest,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?,
    ): RedirectView {
        val redirectUri = redirectUri(request)
        val result = googleOauth2ApplicationService.processOauth2Callback(redirectUri, code, error, state)

        return when (result) {
            is LinkGoogleAccountSuccessDTO -> {
                val redirectView = RedirectView()
                redirectView.url = "meetsama://integration/google/link-account/success"
                redirectView.attributesMap["accountId"] = result.googleAccountId.id
                redirectView
            }
            is LinkGoogleAccountErrorDTO -> {
                val redirectView = RedirectView()
                redirectView.attributesMap["reason"] = result.error
                redirectView.url = "meetsama://integration/google/link-account/error"
                redirectView
            }
            else -> throw UnsupportedOperationException("Invalid callback")
        }
    }

    private fun redirectUri(request: HttpServletRequest): String {
        val genericUrl = GenericUrl(request.requestURL.toString())
        genericUrl.rawPath = "/api/integration/google/callback"
        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
        return genericUrl.build()
    }
}