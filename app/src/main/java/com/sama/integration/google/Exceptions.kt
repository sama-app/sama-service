package com.sama.integration.google

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.sama.common.HasReason
import com.sama.users.domain.UserId


class GoogleInvalidCredentialsException(originalEx: Exception?) :
    RuntimeException("User Google credentials invalid", originalEx), HasReason {
    override val reason: String = "google_invalid_credentials"
}

class GoogleInsufficientPermissionsException(originalEx: Exception) :
    RuntimeException("Insufficient permissions to access Google APIs: ${originalEx.message}", originalEx),
    HasReason {
    override val reason: String = "google_insufficient_permissions"
}

class GoogleUnhandledException(originalEx: GoogleJsonResponseException) :
    RuntimeException("Bad request to Google Calendar: ${originalEx.message}", originalEx)