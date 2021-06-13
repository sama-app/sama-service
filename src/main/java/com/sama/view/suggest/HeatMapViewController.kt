package com.sama.view.suggest

import com.google.api.client.http.GenericUrl
import com.sama.api.config.AuthUserId
import com.sama.suggest.application.SlotSuggestionService
import com.sama.users.application.GoogleOauth2ApplicationService
import com.sama.users.application.GoogleOauth2Failure
import com.sama.users.application.GoogleOauth2Success
import com.sama.users.domain.UserId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.math.round

@Controller
class HeatMapViewController(
    private val googleOauth2ApplicationService: GoogleOauth2ApplicationService,
    private val slotSuggestionService: SlotSuggestionService
) {

    @GetMapping("/__debug/auth/google-authorize")
    fun googleAuthorize(request: HttpServletRequest): RedirectView {
        val googleOAuth2Redirect = googleOauth2ApplicationService.beginGoogleOauth2(redirectUri(request))
        return RedirectView(googleOAuth2Redirect.authorizationUrl)
    }

    @GetMapping("/__debug/auth/google-oauth2")
    fun googleOAuth2Callback(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?
    ) {
        val redirectUri = redirectUri(request)

        val result = googleOauth2ApplicationService.processGoogleOauth2(redirectUri, code, error)
        when (result) {
            is GoogleOauth2Success -> {
                val cookie = Cookie("sama.access", result.accessToken)
                cookie.secure = request.isSecure
                cookie.maxAge = -1
                cookie.path = "/"
                response.addCookie(cookie)
                response.sendRedirect("/__debug/user/heatmap")
            }
            is GoogleOauth2Failure -> response.status = HttpStatus.FORBIDDEN.value()
        }
    }

    @GetMapping("/__debug/user/heatmap")
    fun renderUserHeapMap(@AuthUserId userId: UserId, model: MutableMap<String, Any>): ModelAndView {
        val heatMap = slotSuggestionService.computeHeapMap(userId).value.toSortedMap()
        model["headers"] = heatMap.keys

        // TODO replace this poor implementation of Matrix transpose
        val values = heatMap.values.toList()
        val transposed = MutableList(values[0].size) { MutableList(values.size) { 0 to "" } }
        for (i in values.indices) {
            for (j in values[0].indices) {
                val percentage = (values[i][j] * 100).toInt()
                transposed[j][i] = percentage to percentageToColour(percentage)
            }
        }

        model["vectors"] = transposed
        return ModelAndView("heatmap", model)
    }

    fun percentageToColour(percentage: Int): String {
        var (r, g, b) = listOf(0, 0, 0)
        if (percentage < 50) {
            r = 255
            g = round(5.1 * percentage).toInt()
        } else {
            g = 255
            r = round(510 - 5.1 * percentage).toInt()
        }
        val h = r * 0x10000 + g * 0x100 + b * 0x1
        return "#" + ("000000" + h.toString(16)).takeLast(6)
    }

    private fun redirectUri(request: HttpServletRequest): String {
        val genericUrl = GenericUrl(request.requestURL.toString())
        genericUrl.rawPath = "/__debug/auth/google-oauth2"
        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
        return genericUrl.build()
    }
}