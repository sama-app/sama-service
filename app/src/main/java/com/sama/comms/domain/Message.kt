package com.sama.comms.domain

data class Message(val notification: NotificationData? = null, val additionalData: Map<String, String> = emptyMap())

data class NotificationData(val title: String, val body: String = "")
