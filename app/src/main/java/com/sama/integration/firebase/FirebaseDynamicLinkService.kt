package com.sama.integration.firebase

import java.net.URI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

interface DynamicLinkService {
    fun generate(linkUrl: String): String
}

class FirebaseDynamicLinkService(
    private val restTemplate: RestTemplate,
    private val dynamicLinkConfiguration: FirebaseDynamicLinkConfiguration,
    firebaseApiKey: String,
) : DynamicLinkService {
    private var logger: Logger = LoggerFactory.getLogger(FirebaseDynamicLinkService::class.java)

    private val dynamicLinkApiUri = UriComponentsBuilder.newInstance()
        .uri(URI.create(dynamicLinkConfiguration.apiUrl))
        .queryParam("key", firebaseApiKey)
        .toUriString()

    override fun generate(linkUrl: String): String {
        // Manually constructed Firebase Dynamic Link
        // https://firebase.google.com/docs/dynamic-links/create-manually#parameters
        val urlBuilder = UriComponentsBuilder.newInstance()
            .scheme("https")
            .host(dynamicLinkConfiguration.fqdn)
            .path("/")
            .queryParam("link", linkUrl)
        for ((key, value) in dynamicLinkConfiguration.parameters.entries) {
            if (value.isEmpty()) continue
            urlBuilder.queryParam(key, value)
        }

        val longDynamicLink = urlBuilder.build().toUriString()

        return try {
            // Attempt to shorten the Dynamic link to acquire Analytics as they
            // do not work on manually constructed links
            // https://firebase.google.com/docs/dynamic-links/create-links
            val request = HttpEntity(DynamicLinkShortenRequest(longDynamicLink))
            val response = restTemplate.postForObject(dynamicLinkApiUri,
                request, DynamicLinkShortenResponse::class.java)
            response.shortLink
        } catch (e: Exception) {
            // If that didn't work, still return a long dynamic link
            logger.error("Could not generated short dynamic link", e)
            longDynamicLink
        }

    }
}

private data class DynamicLinkShortenRequest(val longDynamicLink: String)
private data class DynamicLinkShortenResponse(val shortLink: String, val previewLink: String)