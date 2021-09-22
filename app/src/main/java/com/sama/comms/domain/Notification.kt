package com.sama.comms.domain

data class Notification(val title: String, val body: String = "", val additionalData: Map<String, String>)
