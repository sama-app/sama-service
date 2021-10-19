package com.sama.integration.mailerlite

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

interface MailerLiteClient {
    fun addSubscriber(email: String, name: String?)
    fun removeSubscriber(email: String)
}

@Component
class HttpMailerLiteClient(private val mailerLiteRestTemplate: RestTemplate) : MailerLiteClient {
    private var logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun addSubscriber(email: String, name: String?) {
        val request = AddSubscriberRequest(email, name)
        mailerLiteRestTemplate.postForObject("/subscribers", request, MailerLiteSubscriber::class.java)
    }

    override fun removeSubscriber(email: String) {
        val subscriber = try {
            mailerLiteRestTemplate.getForObject("subscribers/$email", MailerLiteSubscriber::class.java)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode != HttpStatus.NOT_FOUND) throw e
            else null
        } ?: return

        if (!subscriber.isSubscribed) return

        val request = UnsubscribeRequest(email, subscriber.name)
        mailerLiteRestTemplate.postForObject("/subscribers", request, MailerLiteSubscriber::class.java)
    }
}

private data class AddSubscriberRequest(
    val email: String,
    val name: String?,
    val resubscribe: Boolean = true,
    val type: MailerLiteSubscriberType = MailerLiteSubscriberType.active,
    val signup_ip: String? = null,
    val signup_timestamp: String? = null
)

private data class UnsubscribeRequest(
    val email: String,
    val name: String?,
    val resubscribe: Boolean = false,
    val type: MailerLiteSubscriberType = MailerLiteSubscriberType.unsubscribed,
)

private data class MailerLiteSubscriber(
    val id: Long,
    val email: String,
    val name: String,
    val type: MailerLiteSubscriberType
) {
    val isSubscribed = type == MailerLiteSubscriberType.active
}

private enum class MailerLiteSubscriberType {
    active, bounced, unsubscribed, junk, unconfirmed
}