package com.sama.integration.google

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.sama.common.HasReason
import com.sama.users.domain.UserId


class GoogleInvalidCredentialsException(val userId: UserId, originalEx: Exception?) :
    RuntimeException("User#${userId} Google credentials invalid", originalEx), HasReason {
    override val reason: String = "google_invalid_credentials"
}

class GoogleInsufficientPermissionsException(val userId: UserId, originalEx: GoogleJsonResponseException) :
    RuntimeException("User#${userId} has insufficient permissions to access Google APIs: ${originalEx.message}", originalEx),
    HasReason {
    override val reason: String = "google_insufficient_permissions"
}

class GoogleUnhandledException(originalEx: GoogleJsonResponseException) :
    RuntimeException("Bad request to Google Calendar: ${originalEx.message}", originalEx)