package com.sama.api.debug

import com.google.api.client.http.GenericUrl
import com.sama.api.config.AuthUserId
import com.sama.auth.application.GoogleOauth2ApplicationService
import com.sama.auth.application.GoogleSignFailureDTO
import com.sama.auth.application.GoogleSignSuccessDTO
import com.sama.common.mapIndexed
import com.sama.slotsuggestion.application.HeatMapServiceV1
import com.sama.slotsuggestion.application.HeatMapServiceV2
import com.sama.slotsuggestion.application.SlotSuggestionRequest
import com.sama.slotsuggestion.application.SlotSuggestionResponse
import com.sama.slotsuggestion.application.SlotSuggestionServiceV1
import com.sama.slotsuggestion.application.SlotSuggestionServiceV2
import com.sama.slotsuggestion.domain.UserRepository
import com.sama.slotsuggestion.domain.v1.WeightContext
import com.sama.slotsuggestion.domain.v1.sigmoid
import com.sama.slotsuggestion.domain.v2.SlotSuggestionEngine
import com.sama.users.domain.UserId
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.math.round
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView

@RestController
class DebugViewController(
    private val googleOauth2ApplicationService: GoogleOauth2ApplicationService,
    private val userRepository: UserRepository,
    private val heatMapService: HeatMapServiceV1,
    private val heatMapServiceV2: HeatMapServiceV2,
    private val weightContext: WeightContext,
    private val slotSuggestionService: SlotSuggestionServiceV1,
    private val slotSuggestionServiceV2: SlotSuggestionServiceV2,
) {

    @GetMapping("/api/__debug/auth/google-authorize")
    fun googleAuthorize(request: HttpServletRequest): RedirectView {
        val googleOAuth2Redirect = googleOauth2ApplicationService.beginGoogleWebOauth2(redirectUri(request))
        return RedirectView(googleOAuth2Redirect.authorizationUrl)
    }

    @GetMapping("/api/__debug/auth/google-oauth2")
    fun googleOAuth2Callback(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
    ) {
        val redirectUri = redirectUri(request)

        val result = googleOauth2ApplicationService.processGoogleWebOauth2(redirectUri, code, error)
        when (result) {
            is GoogleSignSuccessDTO -> {
                val cookie = Cookie("sama.access", result.accessToken)
                cookie.secure = request.isSecure
                cookie.maxAge = -1
                cookie.path = "/"
                response.addCookie(cookie)
                response.sendRedirect("/api/__debug/user/heatmap2?count=3")
            }
            is GoogleSignFailureDTO -> response.status = HttpStatus.FORBIDDEN.value()
        }
    }

    @GetMapping("/api/__debug/user/heatmap")
    fun renderUserHeapMap(@AuthUserId userId: UserId, model: MutableMap<String, Any>): ModelAndView {
        val heatMap = heatMapService.generate(userId, ZoneId.systemDefault())
            .dayVectors()
            .mapValues { it.value.mapIndexed { _, value -> sigmoid(value) } }
            .toSortedMap()

        model["headers"] = listOf(" ") + heatMap.keys.map { it.toString().substring(5) }

        // TODO replace this poor implementation of Matrix transpose
        val values = heatMap.values.toList()
        val transposed = MutableList(values[0].size) { MutableList(values.size) { "0" to "" } }
        for (i in values.indices) {
            for (j in values[0].indices) {
                val percentage = (values[i][j] * 100).toInt()
                transposed[j][i] = percentage.toString() to percentageToColour(percentage)
            }
        }

        for (i in transposed.indices) {
            val time = LocalTime.MIDNIGHT.plusMinutes(i * weightContext.intervalMinutes.toLong())
            transposed[i] =
                (mutableListOf(Pair("${time.hour}:${time.minute}", "#FFFFFF")) + transposed[i]).toMutableList()
        }

        model["vectors"] = transposed
        return ModelAndView("heatmap", model)
    }

    data class Cell(val label: String, val colour: String, val hover: Set<Map.Entry<Any, Double>> = emptySet())

    @GetMapping("/api/__debug/user/heatmap2")
    fun renderUserHeapMap2(
        @AuthUserId userId: UserId,
        @RequestParam(defaultValue = "3") count: Int,
        model: MutableMap<String, Any>,
    ): ModelAndView {
        val user = userRepository.findById(userId)
        val baseHeatMap = heatMapServiceV2.generate(userId, user.timeZone)

        val (suggestedSlots, heatMap) = SlotSuggestionEngine(baseHeatMap)
            .suggest(Duration.ofMinutes(60), count)

        val maxWeight = heatMap.slots.maxOf { it.totalWeight }
        val slotsByDate = heatMap.slots
            .groupBy { it.startDateTime.toLocalDate() }

        model["headers"] = listOf(" ") + slotsByDate.keys.map { it.toString().substring(5) }

        // TODO replace this poor implementation of Matrix transpose
        val values = slotsByDate.values.toList()
        val transposed = MutableList(values[0].size) { MutableList(values.size) { Cell("", "") } }
        for (i in values.indices) {
            for (j in values[0].indices) {
                val slot = values[i][j]

                val isSuggested = suggestedSlots.find { ss ->
                    ss.slots.find {
                        it.startDateTime.isEqual(slot.startDateTime) && it.endDateTime.isEqual(slot.endDateTime)
                    } != null
                } != null

                val weight = String.format("%.2f", slot.totalWeight)
                val sigmoid = sigmoid(x = slot.totalWeight, k = -10 / maxWeight)
                transposed[j][i] = Cell(
                    weight,
                    if (isSuggested) {
                        "#FFFFFF"
                    } else {
                        percentageToColour((sigmoid * 100).toInt())
                    },
                    slot.influences.entries
                )
            }
        }

        for (i in transposed.indices) {
            val time = LocalTime.MIDNIGHT.plusMinutes(i * weightContext.intervalMinutes.toLong())
            transposed[i] =
                (mutableListOf(Cell("${time.hour}:${time.minute}", "#FFFFFF")) + transposed[i]).toMutableList()
        }

        model["vectors"] = transposed
        return ModelAndView("heatmap", model)
    }

    @GetMapping("/api/__debug/user/suggestions", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTopSuggestions(@AuthUserId userId: UserId): SlotSuggestionResponse {
        return slotSuggestionService.suggestSlots(
            userId, SlotSuggestionRequest(
                Duration.ofMinutes(60),
                ZoneId.systemDefault(),
                10
            )
        )
    }


    @GetMapping("/api/__debug/user/suggestions2", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getTopSuggestions2(@AuthUserId userId: UserId): SlotSuggestionResponse {
        return slotSuggestionServiceV2.suggestSlots(
            userId, SlotSuggestionRequest(
                Duration.ofMinutes(60),
                ZoneId.systemDefault(),
                10
            )
        )
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
        genericUrl.rawPath = "/api/__debug/auth/google-oauth2"
        genericUrl.scheme = if (genericUrl.host != "localhost") "https" else "http"
        return genericUrl.build()
    }
}