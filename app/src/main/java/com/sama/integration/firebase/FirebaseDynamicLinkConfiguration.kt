package com.sama.integration.firebase

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.firebase.app-link")
class FirebaseDynamicLinkConfiguration(
    val apiUrl: String,
    val fqdn: String,
    val parameters: Map<String, String>
)